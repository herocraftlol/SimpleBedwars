package com.bedwars.listeners;

import com.bedwars.BedwarsPlugin;
import com.bedwars.util.KitProtectionUtil;
import com.bedwars.util.LobbyItemUtil;
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
 * Empêche toute manipulation des items protégés d'un joueur en partie/lobby :
 *  - l'épée en bois du kit de base (toujours au slot 0) ;
 *  - les 3 items spéciaux du lobby d'attente (diamant "forcer le lancement" au slot 0,
 *    bloc "choisir son équipe" au slot 2, barrière "quitter" au slot 4).
 * Impossible de les drop, déplacer, dupliquer, échanger avec la main secondaire, ou les
 * sortir de quelque façon que ce soit tant que le joueur est en jeu/en lobby.
 */
public class KitProtectionListener implements Listener {

    private final BedwarsPlugin plugin;
    /** Slots protégés dans la hotbar (épée + items du lobby, qui ne se chevauchent jamais). */
    private static final int[] PROTECTED_SLOTS = {0, LobbyItemUtil.SLOT_TEAM_SELECT, LobbyItemUtil.SLOT_LEAVE};

    public KitProtectionListener(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean inGame(Player player) {
        return plugin.getGameManager().findInstanceOf(player) != null;
    }

    private boolean isLocked(ItemStack item) {
        return KitProtectionUtil.isProtectedSword(item) || LobbyItemUtil.isAnyLobbyItem(item);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isLocked(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (isLocked(event.getMainHandItem()) || isLocked(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !inGame(player)) return;
        if (isLocked(event.getOldCursor())) {
            event.setCancelled(true);
            return;
        }
        for (ItemStack item : event.getNewItems().values()) {
            if (isLocked(item)) {
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

        // Les slots protégés eux-mêmes : jamais touchables (empêche de les remplacer / les sortir).
        if (event.getClickedInventory() instanceof PlayerInventory) {
            for (int protectedSlot : PROTECTED_SLOTS) {
                if (event.getSlot() == protectedSlot && isLocked(event.getClickedInventory().getItem(protectedSlot))) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // L'item cliqué ou sous le curseur est protégé : bloque tout déplacement.
        if (isLocked(event.getCurrentItem()) || isLocked(event.getCursor())) {
            event.setCancelled(true);
            return;
        }

        // Échange via une touche numérique (1-9) impliquant un slot protégé de la hotbar.
        for (int protectedSlot : PROTECTED_SLOTS) {
            if (event.getHotbarButton() == protectedSlot && isLocked(player.getInventory().getItem(protectedSlot))) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
