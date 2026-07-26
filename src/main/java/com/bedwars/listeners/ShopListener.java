package com.bedwars.listeners;

import com.bedwars.BedwarsPlugin;
import com.bedwars.shop.ShopGUIManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ShopListener implements Listener {

    private final BedwarsPlugin plugin;

    public ShopListener(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ShopGUIManager.ShopGUIHolder holder)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getSlot();
        plugin.getShopGUIManager().handleClick(player, holder, slot);
    }
}
