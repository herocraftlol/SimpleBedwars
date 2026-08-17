package com.bedwars.listeners;

import com.bedwars.BedwarsPlugin;
import com.bedwars.util.KitProtectionUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Empêche toute manipulation de l'épée en bois protégée (kit de base, toujours au
 * slot 0 de la hotbar) : impossible de la drop, déplacer, dupliquer, échanger avec la
 * main secondaire, ou en sortir de quelque façon que ce soit tant qu'une partie est en cours.
 */
public class KitProtectionListener implements Listener {

    private final BedwarsPlugin plugin;

    public KitProtectionListener(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean inGame(Player player) {
        return plugin.getGameManager().findInstanceOf(player) != null;
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (KitProtectionUtil.isProtectedSword(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (KitProtectionUtil.isProtectedSword(event.getMainHandItem())
                || KitProtectionUtil.isProtectedSword(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !inGame(player)) return;
        if (KitProtectionUtil.isProtectedSword(event.getOldCursor())) {
            event.setCancelled(true);
            return;
        }
        for (ItemStack item : event.getNewItems().values()) {
            if (KitProtectionUtil.isProtectedSword(item)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !inGame(player)) return;
        if (!(event.getInventory() instanceof PlayerInventory)
                && !(event.getClickedInventory() instanceof PlayerInventory)) {
            return; // pas l'inventaire du joueur (un shop/GUI custom est déjà géré ailleurs)
        }

        // Le slot 0 lui-même : jamais touchable (empêche de le remplacer / le sortir).
        if (event.getSlot() == 0 && event.getClickedInventory() instanceof PlayerInventory) {
            ItemStack currentAtZero = event.getClickedInventory().getItem(0);
            if (KitProtectionUtil.isProtectedSword(currentAtZero)) {
                event.setCancelled(true);
                return;
            }
        }

        // L'item cliqué ou sous le curseur est l'épée protégée : bloque tout déplacement.
        if (KitProtectionUtil.isProtectedSword(event.getCurrentItem())
                || KitProtectionUtil.isProtectedSword(event.getCursor())) {
            event.setCancelled(true);
            return;
        }

        // Échange via une touche numérique (1-9) impliquant le slot 0 de la hotbar.
        if (event.getHotbarButton() == 0) {
            ItemStack hotbarSlotZero = player.getInventory().getItem(0);
            if (KitProtectionUtil.isProtectedSword(hotbarSlotZero)) {
                event.setCancelled(true);
            }
        }
    }
}
