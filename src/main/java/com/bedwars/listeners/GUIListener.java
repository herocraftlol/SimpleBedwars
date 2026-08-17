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

/**
 * Gère le NPC hub (clic -> ouvre le GUI paginé des arènes) ainsi que les clics à
 * l'intérieur de ce GUI : rejoindre une arène disponible, observer une partie en cours,
 * naviguer entre les pages, ou rejoindre une arène aléatoire — sur le même principe que
 * le GUI d'arènes de HikaBrain.
 */
public class GUIListener implements Listener {

    private final BedwarsPlugin plugin;

    public GUIListener(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNpcInteract(PlayerInteractEntityEvent event) {
        if (!plugin.getAdminNPCManager().isNPC(event.getRightClicked().getUniqueId())) return;
        event.setCancelled(true);
        // Le hub liste toutes les parties et permet à n'importe quel joueur de les rejoindre.
        plugin.getAdminGUIManager().open(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!AdminGUIManager.isArenaGuiTitle(title)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        int slot = event.getRawSlot();
        int page = AdminGUIManager.parsePageFromTitle(title);

        if (AdminGUIManager.isPrevPageButton(slot)) {
            plugin.getAdminGUIManager().open(player, page - 1);
            return;
        }
        if (AdminGUIManager.isNextPageButton(slot)) {
            plugin.getAdminGUIManager().open(player, page + 1);
            return;
        }
        if (AdminGUIManager.isRandomButton(slot)) {
            player.closeInventory();
            Arena best = plugin.getAdminGUIManager().findBestArenaForRandomJoin();
            if (best == null) {
                player.sendMessage("§cAucune arène disponible pour le moment.");
                return;
            }
            tryJoin(player, best);
            return;
        }

        Arena arena = plugin.getAdminGUIManager().getArenaNameAt(page, slot);
        if (arena == null) return; // slot vide / filler

        player.closeInventory();

        boolean running = arena.getState() == ArenaState.PLAYING || arena.getState() == ArenaState.SUDDEN_DEATH;
        if (running) {
            trySpectate(player, arena);
        } else {
            tryJoin(player, arena);
        }
    }

    private void tryJoin(Player player, Arena arena) {
        if (!arena.isSaved() || arena.getState() == ArenaState.SETUP) {
            player.sendMessage("§cCette arène n'est pas encore configurée.");
            return;
        }
        if (!plugin.getGameManager().joinArena(player, arena)) {
            player.sendMessage("§cImpossible de rejoindre cette partie (pleine ou déjà lancée).");
        }
    }

    private void trySpectate(Player player, Arena arena) {
        GameInstance instance = plugin.getGameManager().getExistingInstance(arena);
        if (instance != null) {
            instance.toSpectator(player);
            player.sendMessage("§7Vous observez maintenant la partie §b" + arena.getName());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (AdminGUIManager.isArenaGuiTitle(event.getView().getTitle()) && event.getPlayer() instanceof Player player) {
            plugin.getAdminGUIManager().onClose(player);
        }
    }
}
