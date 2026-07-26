package com.bedwars.listeners;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.game.GameInstance;
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

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        GameInstance game = plugin.getGameManager().findInstanceOf(event.getPlayer());
        if (game == null) return;
        game.trackPlacedBlock(event.getBlock().getLocation());
    }

    /** Empêche les spectateurs de sortir de la cage de spectateurs (mode Spectateur = vol/no-clip natif). */
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SPECTATOR) return;
        GameInstance game = plugin.getGameManager().findInstanceOf(player);
        if (game == null) return;
        Arena arena = game.getArena();
        if (arena.getSpecLocation() == null) return;

        Location to = event.getTo();
        if (to == null) return;
        double radius = plugin.getConfig().getInt("spectator.cage-radius", 4) + 1.0;
        if (!isNear(to, arena.getSpecLocation(), radius)) {
            event.setTo(arena.getSpecLocation());
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
            game.removeWaitingPlayer(event.getPlayer());
        }
    }
}
