package com.bedwars.listeners;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.ArenaState;
import com.bedwars.game.GameInstance;
import com.bedwars.gui.JoinGUIManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class GUIListener implements Listener {

    private final BedwarsPlugin plugin;

    public GUIListener(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof JoinGUIManager.JoinGUIHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Arena arena = plugin.getJoinGUIManager().resolveArenaFromItem(event.getCurrentItem());
        if (arena == null) return;

        boolean running = arena.getState() == ArenaState.PLAYING || arena.getState() == ArenaState.SUDDEN_DEATH;
        if (running) {
            GameInstance instance = plugin.getGameManager().getExistingInstance(arena);
            if (instance != null) {
                instance.toSpectator(player);
                player.sendMessage("§7Vous observez maintenant la partie §b" + arena.getName());
                player.closeInventory();
            }
        } else {
            player.closeInventory();
            if (!plugin.getGameManager().joinArena(player, arena)) {
                player.sendMessage("§cImpossible de rejoindre cette partie (pleine ou déjà lancée).");
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof JoinGUIManager.JoinGUIHolder
                && event.getPlayer() instanceof Player player) {
            plugin.getJoinGUIManager().onClose(player);
        }
    }
}
