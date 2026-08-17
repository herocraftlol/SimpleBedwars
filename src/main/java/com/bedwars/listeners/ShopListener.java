package com.bedwars.listeners;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.npc.ShopNpcManager;
import com.bedwars.shop.ShopGUIManager;
import com.bedwars.upgrade.UpgradeGUIManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

/**
 * Gère le clic sur les PNJ marchand/amélioration (ouverture du bon GUI) ainsi que
 * les clics à l'intérieur de ces GUIs (achats).
 */
public class ShopListener implements Listener {

    private final BedwarsPlugin plugin;

    public ShopListener(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNpcInteract(PlayerInteractAtEntityEvent event) {
        ShopNpcManager.NpcInfo info = plugin.getShopNpcManager().getInfo(event.getRightClicked().getUniqueId());
        if (info == null) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        Arena arena = plugin.getArenaManager().getArena(info.arenaName());
        if (arena == null) return;

        if (info.type() == ShopNpcManager.NpcType.SHOP) {
            plugin.getShopGUIManager().open(player, arena, info.team());
        } else {
            plugin.getUpgradeGUIManager().open(player, arena, info.team());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        var topInventory = event.getView().getTopInventory();
        boolean isShop = topInventory.getHolder() instanceof ShopGUIManager.ShopHolder;
        boolean isUpgrade = topInventory.getHolder() instanceof UpgradeGUIManager.UpgradeHolder
                || topInventory.getHolder() instanceof UpgradeGUIManager.TrapPickHolder;
        if (!isShop && !isUpgrade) return;

        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(topInventory)) return;

        int slot = event.getRawSlot();

        if (isShop) {
            plugin.getShopGUIManager().handleClick(player, topInventory, slot);
            return;
        }

        String arenaName;
        if (topInventory.getHolder() instanceof UpgradeGUIManager.UpgradeHolder h) {
            arenaName = h.getArenaName();
        } else {
            arenaName = ((UpgradeGUIManager.TrapPickHolder) topInventory.getHolder()).getArenaName();
        }
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena != null) {
            plugin.getUpgradeGUIManager().handleClick(player, arena, topInventory, slot);
        }
    }
}
