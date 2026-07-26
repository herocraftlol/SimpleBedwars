package com.bedwars.gui;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.ArenaState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Menu joueur "/bd gui" listant toutes les parties disponibles/en cours.
 * Remplace l'ancien "/bd admin gui" (supprimé) : accessible à tout le monde,
 * pas seulement aux admins. Les emplacements sont remplis dans l'ordre normal
 * de lecture : de haut en bas, de gauche à droite (slot 0 à 53).
 */
public class JoinGUIManager {

    public static final String TITLE = ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Parties Bedwars";
    private static final int SIZE = 54;

    private final BedwarsPlugin plugin;
    private final Set<Player> viewers = new HashSet<>();

    public JoinGUIManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, 20L, 20L);
    }

    public void open(Player player) {
        Inventory inv = build();
        viewers.add(player);
        player.openInventory(inv);
    }

    public void onClose(Player player) {
        viewers.remove(player);
    }

    private void refreshAll() {
        for (Player player : new ArrayList<>(viewers)) {
            if (player.getOpenInventory() == null
                    || !ChatColor.stripColor(player.getOpenInventory().getTitle()).equals(ChatColor.stripColor(TITLE))) {
                viewers.remove(player);
                continue;
            }
            populate(player.getOpenInventory().getTopInventory());
        }
    }

    private Inventory build() {
        Inventory inv = Bukkit.createInventory(new JoinGUIHolder(), SIZE, TITLE);
        populate(inv);
        return inv;
    }

    private void populate(Inventory inv) {
        ItemStack grayPane = pane(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "Emplacement libre", List.of());
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, grayPane);
        }

        List<Arena> arenas = new ArrayList<>(plugin.getArenaManager().getArenas().values());
        int index = 0;
        for (Arena arena : arenas) {
            if (!arena.isSaved()) continue; // n'affiche que les arènes jouables
            if (index >= SIZE) break; // plus de place dans le GUI
            inv.setItem(index, buildArenaItem(arena));
            index++;
        }
    }

    private ItemStack buildArenaItem(Arena arena) {
        int max = Math.max(arena.getMaxPlayers(), 0);
        int current = arena.getCurrentPlayerCount();
        boolean running = arena.getState() == ArenaState.PLAYING || arena.getState() == ArenaState.SUDDEN_DEATH;

        Material material = running ? Material.RED_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE;
        String stateLabel = switch (arena.getState()) {
            case SETUP -> ChatColor.GRAY + "Non configurée";
            case WAITING -> ChatColor.GREEN + "En attente de joueurs";
            case STARTING -> ChatColor.YELLOW + "Démarrage imminent";
            case PLAYING -> ChatColor.RED + "Partie en cours";
            case SUDDEN_DEATH -> ChatColor.DARK_RED + "Mort subite";
            case ENDING -> ChatColor.GRAY + "Réinitialisation...";
        };

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Emplacements: " + ChatColor.WHITE + current + "/" + max);
        lore.add(ChatColor.GRAY + "État: " + stateLabel);
        lore.add("");
        lore.add(running ? ChatColor.YELLOW + "Clique pour observer en spectateur"
                : ChatColor.YELLOW + "Clique pour rejoindre");

        return pane(material, ChatColor.AQUA + "" + ChatColor.BOLD + arena.getName(), lore);
    }

    private ItemStack pane(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public Arena resolveArenaFromItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null || item.getItemMeta().getDisplayName() == null) return null;
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        return plugin.getArenaManager().getArena(name);
    }

    public static class JoinGUIHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
