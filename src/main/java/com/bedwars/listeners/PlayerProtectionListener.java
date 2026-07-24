package com.bedwars.listeners;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.game.GameInstance;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.GameMode;

public class PlayerProtectionListener implements Listener {

    private final BedwarsPlugin plugin;

    public PlayerProtectionListener(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        GameInstance game = plugin.getGameManager().findInstanceOf(event.getPlayer());
        if (game == null) return;
        game.trackPlacedBlock(event.getBlock().getLocation());
    }

    /** Empêche les spectateurs (et tout joueur mort sans lit) de sortir de la zone de spectateurs. */
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SPECTATOR) return;
        GameInstance game = plugin.getGameManager().findInstanceOf(player);
        if (game == null) return;
        Arena arena = game.getArena();
        if (arena.getLobbyPos1() == null || arena.getLobbyPos2() == null || arena.getSpecLocation() == null) return;

        Location to = event.getTo();
        if (to == null) return;
        if (!isWithin(to, arena.getLobbyPos1(), arena.getLobbyPos2())
                && !isNear(to, arena.getSpecLocation(), 60)) {
            // Le spectateur essaie de sortir trop loin de la zone prévue : on le replace.
            event.setTo(arena.getSpecLocation());
        }
    }

    private boolean isNear(Location loc, Location center, double radius) {
        if (!loc.getWorld().equals(center.getWorld())) return false;
        return loc.distanceSquared(center) <= radius * radius;
    }

    private boolean isWithin(Location loc, Location pos1, Location pos2) {
        if (!loc.getWorld().equals(pos1.getWorld())) return false;
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        return loc.getX() >= minX && loc.getX() <= maxX
                && loc.getY() >= minY && loc.getY() <= maxY
                && loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        GameInstance game = plugin.getGameManager().findInstanceOf(event.getPlayer());
        if (game != null) {
            game.removeWaitingPlayer(event.getPlayer());
        }
    }
}
