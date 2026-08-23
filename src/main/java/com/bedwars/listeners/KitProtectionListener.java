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
 * Deux niveaux de protection :
 *  - Les VRAIS boutons fonctionnels du lobby (diamant "forcer le lancement", barrière "quitter") :
 *    totalement verrouillés (indroppables, indéplaçables), comme avant.
 *  - L'épée, la pioche, la hache et le bloc "choisir son équipe" : placés au bon slot de la hotbar
 *    (celui choisi via /bd quickmenu, sinon le défaut) au moment où ils sont donnés (spawn,
 *    réapparition, achat — voir GameInstance#placeAtPreferredSlot), mais ENSUITE librement
 *    réorganisables PAR LE JOUEUR À L'INTÉRIEUR DE SA HOTBAR (échanger deux slots de hotbar entre
 *    eux). Toujours indroppables et impossibles à sortir de la hotbar (vers l'inventaire principal
 *    ou un autre GUI), pour ne jamais les perdre.
 */
public class KitProtectionListener implements Listener {

    private final BedwarsPlugin plugin;

    public KitProtectionListener(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean inGame(Player player) {
        return plugin.getGameManager().findInstanceOf(player) != null;
    }

    /** Boutons fonctionnels : jamais déplaçables, jamais droppables. */
    private boolean isFullyLocked(ItemStack item) {
        return LobbyItemUtil.isProtectedFromMoving(item);
    }

    /** Épée/pioche/hache/bloc d'équipe : déplaçables, mais uniquement au sein de la hotbar. */
    private boolean isHotbarOnly(ItemStack item) {
        return KitProtectionUtil.isKitTool(item) || LobbyItemUtil.isTeamSelect(item);
    }

    private boolean isAnySpecial(ItemStack item) {
        return isFullyLocked(item) || isHotbarOnly(item);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isAnySpecial(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (isAnySpecial(event.getMainHandItem()) || isAnySpecial(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !inGame(player)) return;
        if (isAnySpecial(event.getOldCursor())) {
            event.setCancelled(true);
            return;
        }
        for (ItemStack item : event.getNewItems().values()) {
            if (isAnySpecial(item)) {
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

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        // Boutons fonctionnels : jamais touchables, quel que soit le clic.
        if (isFullyLocked(current) || isFullyLocked(cursor)) {
            event.setCancelled(true);
            return;
        }

        // Épée/outils/bloc équipe : autorisé seulement si ça reste dans la hotbar (slots 0-8),
        // pas de shift-click (qui les enverrait dans l'inventaire principal).
        if (isHotbarOnly(current) || isHotbarOnly(cursor)) {
            boolean clickedIsHotbar = event.getClickedInventory() instanceof PlayerInventory
                    && event.getSlot() >= 0 && event.getSlot() <= 8;
            if (event.getClick().isShiftClick() || !clickedIsHotbar) {
                event.setCancelled(true);
            }
            return;
        }

        // Échange via une touche numérique (1-9) : le slot de hotbar visé doit lui aussi être
        // protégé de la même façon (jamais sorti de la hotbar, jamais s'il s'agit d'un bouton).
        if (event.getHotbarButton() >= 0) {
            ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
            if (isFullyLocked(hotbarItem)) {
                event.setCancelled(true);
            } else if (isHotbarOnly(hotbarItem)) {
                boolean clickedIsHotbar = event.getClickedInventory() instanceof PlayerInventory
                        && event.getSlot() >= 0 && event.getSlot() <= 8;
                if (!clickedIsHotbar) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
