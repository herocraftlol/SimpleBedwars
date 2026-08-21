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
 *  - les 3 outils du kit de base (épée/hache/pioche — voir KitProtectionUtil) ;
 *  - les 3 items spéciaux du lobby d'attente (diamant "forcer le lancement", bloc "choisir son
 *    équipe", barrière "quitter" — voir LobbyItemUtil).
 * Impossible de les drop, déplacer, dupliquer, échanger avec la main secondaire, ou les
 * sortir de quelque façon que ce soit tant que le joueur est en jeu/en lobby.
 *
 * Volontairement basé sur le CONTENU (tag) des items plutôt que sur des numéros de slot fixes :
 * chaque joueur peut personnaliser la position de ses items via /bd quickmenu (voir
 * PlayerPrefsManager), donc la protection doit suivre l'item où qu'il se trouve.
 */
public class KitProtectionListener implements Listener {

    private final BedwarsPlugin plugin;

    public KitProtectionListener(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean inGame(Player player) {
        return plugin.getGameManager().findInstanceOf(player) != null;
    }

    private boolean isLocked(ItemStack item) {
        return KitProtectionUtil.isKitTool(item) || LobbyItemUtil.isAnyLobbyItem(item);
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

        // L'item cliqué ou sous le curseur est protégé : bloque tout déplacement, quel que soit
        // le slot où il se trouve (personnalisable via /bd quickmenu).
        if (isLocked(event.getCurrentItem()) || isLocked(event.getCursor())) {
            event.setCancelled(true);
            return;
        }

        // Échange via une touche numérique (1-9) : vérifie le contenu réel du slot de hotbar visé,
        // peu importe lequel (pas de liste figée, pour suivre les positions personnalisées).
        if (event.getHotbarButton() >= 0) {
            ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
            if (isLocked(hotbarItem)) {
                event.setCancelled(true);
            }
        }
    }
}
