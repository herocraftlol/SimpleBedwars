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

        // Joueur vivant en partie : mort instantanée dès qu'il sort de la zone de jeu par en
        // dessous (pas besoin d'attendre qu'il tombe jusque dans le vide du monde).
        if (game.isAlivePlaying(player) && arena.getGamePos1() != null && arena.getGamePos2() != null) {
            double minY = Math.min(arena.getGamePos1().getY(), arena.getGamePos2().getY());
            if (player.getLocation().getY() < minY - 1) {
                player.setHealth(0.0);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        GameInstance game = plugin.getGameManager().findInstanceOf(event.getPlayer());
        if (game != null) {
            game.handlePlayerLeave(event.getPlayer());
        }
    }
}
