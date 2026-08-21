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
                && !isWithinGameZone(arena, event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Vous ne pouvez pas construire en dehors de la zone de jeu.");
            return;
        }

        game.trackPlacedBlock(event.getBlock().getLocation());
    }

    private boolean isWithinGameZone(Arena arena, Location blockLoc) {
        Location pos1 = arena.getGamePos1();
        Location pos2 = arena.getGamePos2();
        if (pos1 == null || pos2 == null || !blockLoc.getWorld().equals(pos1.getWorld())) return true;

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        int bx = blockLoc.getBlockX(), by = blockLoc.getBlockY(), bz = blockLoc.getBlockZ();
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
            Location center = arena.getSpectatorSpawnLocation() != null
                    ? arena.getSpectatorSpawnLocation() : arena.getSpecLocation();
            if (center == null) return;

            Location to = event.getTo();
            if (to == null) return;
            if (!plugin.getWaitingLobbyManager().isWithin(arena, to) && !isNear(to, center, 60)) {
                // Le spectateur essaie de sortir trop loin de la zone prévue : on le replace.
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

    private boolean isNear(Location loc, Location center, double radius) {
        if (!loc.getWorld().equals(center.getWorld())) return false;
        return loc.distanceSquared(center) <= radius * radius;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        GameInstance game = plugin.getGameManager().findInstanceOf(event.getPlayer());
        if (game != null) {
            game.handlePlayerLeave(event.getPlayer());
        }
    }
}
