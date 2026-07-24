package com.bedwars.gui;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.ArenaState;
import com.bedwars.game.GameInstance;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdminGUIManager {

    public static final String TITLE = ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Administration Bedwars";
    private static final int SIZE = 54; // taille d'un double coffre

    private final BedwarsPlugin plugin;
    private final Set<Player> viewers = new HashSet<>();

    public AdminGUIManager(BedwarsPlugin plugin) {
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
            Inventory inv = player.getOpenInventory().getTopInventory();
            populate(inv);
        }
    }

    private Inventory build() {
        Inventory inv = Bukkit.createInventory(new AdminGUIHolder(), SIZE, TITLE);
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
            int slot = slotFor(index);
            if (slot < 0) break; // plus de place dans le GUI (>54 arènes)
            inv.setItem(slot, buildArenaItem(arena));
            index++;
        }
    }

    private int slotFor(int arenaIndex) {
        int row = 5 - (arenaIndex / 9);
        int col = arenaIndex % 9;
        if (row < 0) return -1;
        return row * 9 + col;
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
        if (running) {
            lore.add("");
            lore.add(ChatColor.YELLOW + "Clique pour observer en spectateur");
        }

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

    /** Récupère l'arène correspondant au nom affiché dans un item du GUI. */
    public Arena resolveArenaFromItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null || item.getItemMeta().getDisplayName() == null) return null;
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        return plugin.getArenaManager().getArena(name);
    }

    public static class AdminGUIHolder implements org.bukkit.inventory.InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
