package com.bedwars.listeners;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.ArenaState;
import com.bedwars.game.GameInstance;
import com.bedwars.gui.AdminGUIManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class GUIListener implements Listener {

    private final BedwarsPlugin plugin;

    public GUIListener(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNpcInteract(PlayerInteractEntityEvent event) {
        if (!plugin.getAdminNPCManager().isNPC(event.getRightClicked().getUniqueId())) return;
        event.setCancelled(true);
        if (!event.getPlayer().hasPermission("bedwars.admin")) {
            event.getPlayer().sendMessage("§cVous n'avez pas la permission d'utiliser ce menu.");
            return;
        }
        plugin.getAdminGUIManager().open(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AdminGUIManager.AdminGUIHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Arena arena = plugin.getAdminGUIManager().resolveArenaFromItem(event.getCurrentItem());
        if (arena == null) return;

        boolean running = arena.getState() == ArenaState.PLAYING || arena.getState() == ArenaState.SUDDEN_DEATH;
        if (running) {
            GameInstance instance = plugin.getGameManager().getExistingInstance(arena);
            if (instance != null) {
                instance.toSpectator(player);
                player.sendMessage("§7Vous observez maintenant la partie §b" + arena.getName());
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof AdminGUIManager.AdminGUIHolder
                && event.getPlayer() instanceof Player player) {
            plugin.getAdminGUIManager().onClose(player);
        }
    }
}
