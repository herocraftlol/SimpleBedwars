package com.bedwars.listeners;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.ArenaState;
import com.bedwars.game.GameInstance;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;

public class PlayerProtectionListener implements Listener {

    private final BedwarsPlugin plugin;

    public PlayerProtectionListener(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Interdit de construire en dehors de la zone de jeu : en hauteur, sur les côtés, ou en dessous. */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        GameInstance game = plugin.getGameManager().findInstanceOf(event.getPlayer());
        if (game == null) return;
        Arena arena = game.getArena();

        if ((arena.getState() == ArenaState.PLAYING || arena.getState() == ArenaState.SUDDEN_DEATH)
                && !isWithinGameZone(arena, event.getBlock().getLocation(), 0, 0)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Vous ne pouvez pas construire en dehors de la zone de jeu.");
            return;
        }

        game.trackPlacedBlock(event.getBlock().getLocation());
    }

    /**
     * true si loc est dans la zone de jeu (pos1/pos2), avec une marge optionnelle : {@code marginXZ}
     * de chaque côté (X/Z), {@code marginY} au-dessus ET en dessous. Marges à 0 = zone stricte
     * (utilisé pour interdire de construire hors-zone) ; marges larges = pour laisser les
     * spectateurs se déplacer dans toute la map sans être bridés au moindre pas de trop.
     */
    private boolean isWithinGameZone(Arena arena, Location loc, int marginXZ, int marginY) {
        Location pos1 = arena.getGamePos1();
        Location pos2 = arena.getGamePos2();
        if (pos1 == null || pos2 == null || !loc.getWorld().equals(pos1.getWorld())) return true;

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX()) - marginXZ;
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX()) + marginXZ;
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY()) - marginY;
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY()) + marginY;
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ()) - marginXZ;
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ()) + marginXZ;

        int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();
        return bx >= minX && bx <= maxX && by >= minY && by <= maxY && bz >= minZ && bz <= maxZ;
    }

    /** Empêche les spectateurs (et tout joueur mort sans lit) de sortir de la zone de spectateurs. */
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        GameInstance game = plugin.getGameManager().findInstanceOf(player);
        if (game == null) return;
        Arena arena = game.getArena();

        if (player.getGameMode() == GameMode.SPECTATOR) {
            Location to = event.getTo();
            if (to == null) return;

            boolean inLobbyCage = plugin.getWaitingLobbyManager().isWithin(arena, to);
            boolean inGameZone = isWithinGameZone(arena, to, 20, 30);
            if (inLobbyCage || inGameZone) return;

            // Ni dans la cage du lobby, ni dans la zone de jeu (même élargie) : trop loin, on replace.
            Location center = arena.getSpectatorSpawnLocation() != null
                    ? arena.getSpectatorSpawnLocation() : arena.getSpecLocation();
            if (center != null) {
                event.setTo(center);
            }
            return;
        }

        // Joueur vivant en partie : mort instantanée s'il tombe nettement en dessous de la zone
        // jouable (pas besoin d'attendre qu'il tombe jusque dans le vide du monde). On se base
        // sur les spawns/lits des équipes (toujours fiables, précisément au niveau du sol jouable)
        // plutôt que sur pos1/pos2 (qui ne délimitent pas forcément le sol au bloc près et
        // provoquaient des morts en pleine partie dès le moindre mouvement).
        if (game.isAlivePlaying(player)) {
            double threshold = computeDeathThresholdY(arena);
            if (threshold != Double.NEGATIVE_INFINITY && player.getLocation().getY() < threshold) {
                player.setHealth(0.0);
            }
        }
    }

    /** Seuil de Y en dessous duquel un joueur meurt instantanément (grosse marge de sécurité). */
    private double computeDeathThresholdY(Arena arena) {
        double minY = Double.MAX_VALUE;
        for (com.bedwars.arena.ArenaTeam team : arena.getTeams().values()) {
            if (team.getSpawnLocation() != null) minY = Math.min(minY, team.getSpawnLocation().getY());
            if (team.getBedLocation() != null) minY = Math.min(minY, team.getBedLocation().getY());
        }
        if (minY == Double.MAX_VALUE) {
            if (arena.getGamePos1() == null || arena.getGamePos2() == null) return Double.NEGATIVE_INFINITY;
            minY = Math.min(arena.getGamePos1().getY(), arena.getGamePos2().getY());
        }
        return minY - 15; // grosse marge : ne se déclenche qu'en cas de vraie chute hors de la map
    }

    /**
     * Un coffre enregistré via /bd <nom> chest <couleur> est privé à son équipe : les autres
     * joueurs ne peuvent pas l'ouvrir tant que cette équipe n'est pas totalement éliminée.
     */
    @EventHandler
    public void onChestInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        org.bukkit.block.Block block = event.getClickedBlock();
        if (block == null) return;
        if (block.getType() != org.bukkit.Material.CHEST && block.getType() != org.bukkit.Material.TRAPPED_CHEST) return;

        Player player = event.getPlayer();
        GameInstance game = plugin.getGameManager().findInstanceOf(player);
        if (game == null) return;
        Arena arena = game.getArena();

        com.bedwars.arena.TeamColor owner = findOwningTeam(arena, block.getLocation());
        if (owner == null) return; // pas un coffre d'équipe enregistré : pas concerné

        com.bedwars.arena.TeamColor playerTeam = game.getTeamColor(player);
        if (playerTeam == owner) return; // son propre coffre : toujours autorisé

        com.bedwars.arena.ArenaTeam ownerTeam = arena.getTeams().get(owner);
        if (ownerTeam != null && ownerTeam.isEliminated()) return; // équipe éliminée : coffre pillable

        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "Ce coffre appartient à l'équipe " + owner.getColoredName()
                + ChatColor.RED + " : accessible seulement une fois cette équipe totalement éliminée.");
    }

    /**
     * Retrouve l'équipe propriétaire d'un coffre à partir de sa position (tolère 1 bloc d'écart
     * pour couvrir la seconde moitié d'un coffre double, même si seule une moitié a été enregistrée
     * via /bd <nom> chest).
     */
    private com.bedwars.arena.TeamColor findOwningTeam(Arena arena, Location chestLoc) {
        for (Map.Entry<com.bedwars.arena.TeamColor, com.bedwars.arena.ArenaTeam> entry : arena.getTeams().entrySet()) {
            for (Location registered : entry.getValue().getChestLocations()) {
                if (registered.getWorld() == null || !registered.getWorld().equals(chestLoc.getWorld())) continue;
                boolean exact = registered.getBlockX() == chestLoc.getBlockX()
                        && registered.getBlockY() == chestLoc.getBlockY()
                        && registered.getBlockZ() == chestLoc.getBlockZ();
                boolean adjacent = !exact
                        && registered.getBlockY() == chestLoc.getBlockY()
                        && Math.abs(registered.getBlockX() - chestLoc.getBlockX()) <= 1
                        && Math.abs(registered.getBlockZ() - chestLoc.getBlockZ()) <= 1;
                if (exact || adjacent) return entry.getKey();
            }
        }
        return null;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        GameInstance game = plugin.getGameManager().findInstanceOf(event.getPlayer());
        if (game != null) {
            game.handlePlayerLeave(event.getPlayer());
        }
    }
}
