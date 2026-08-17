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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GUI paginé listant toutes les arènes disponibles, sur le même principe que le GUI
 * d'arènes de HikaBrain :
 *   - Lignes 1 à 5 (slots 0-44) : une icône par arène (45 par page, en plusieurs pages
 *     si besoin)
 *   - Ligne 6 : slot 45 = page précédente, slots 46-52 = bouton "arène aléatoire"
 *     (fonctionne sur toutes les arènes, peu importe la page affichée), slot 53 = page suivante
 */
public class AdminGUIManager {

    public static final String TITLE_BASE = ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "⚔ Bedwars" + ChatColor.RESET
            + ChatColor.GRAY + " - Parties disponibles";

    private static final int GUI_SIZE = 54;
    private static final int PAGE_SIZE = 45;
    private static final int SLOT_PREV_PAGE = 45;
    private static final int SLOT_RANDOM_START = 46;
    private static final int SLOT_RANDOM_END = 52;
    private static final int SLOT_NEXT_PAGE = 53;

    private static final Pattern PAGE_TITLE_PATTERN = Pattern.compile("\\((\\d+)/(\\d+)\\)");

    private final BedwarsPlugin plugin;

    public AdminGUIManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        player.openInventory(buildInventory(page));
    }

    public void onClose(Player player) {
        // Rien à nettoyer : chaque ouverture reconstruit un inventaire frais.
    }

    // ── Placement des arènes sur les pages ────────────────────────────────────

    private Map<Integer, Map<Integer, Arena>> computePlacement() {
        List<Arena> allArenas = new ArrayList<>(plugin.getArenaManager().getArenas().values());
        Map<Integer, Map<Integer, Arena>> placement = new LinkedHashMap<>();

        int page = 0;
        int slot = 0;
        for (Arena arena : allArenas) {
            placement.computeIfAbsent(page, k -> new LinkedHashMap<>()).put(slot, arena);
            slot++;
            if (slot >= PAGE_SIZE) {
                slot = 0;
                page++;
            }
        }
        return placement;
    }

    private int getTotalPages() {
        int maxPage = 0;
        for (Integer p : computePlacement().keySet()) maxPage = Math.max(maxPage, p);
        return maxPage + 1;
    }

    private String titleFor(int page, int totalPages) {
        if (totalPages <= 1) return TITLE_BASE;
        return TITLE_BASE + ChatColor.RESET + ChatColor.GRAY + " (" + (page + 1) + "/" + totalPages + ")";
    }

    public Inventory buildInventory(int page) {
        int totalPages = getTotalPages();
        if (page < 0) page = 0;
        if (page > totalPages - 1) page = totalPages - 1;

        Inventory inv = Bukkit.createInventory(new AdminGUIHolder(), GUI_SIZE, titleFor(page, totalPages));

        Map<Integer, Arena> pagePlacement = computePlacement().getOrDefault(page, Collections.emptyMap());
        for (Map.Entry<Integer, Arena> entry : pagePlacement.entrySet()) {
            inv.setItem(entry.getKey(), buildArenaItem(entry.getValue()));
        }

        ItemStack filler = pane(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < PAGE_SIZE; i++) {
            if (!pagePlacement.containsKey(i)) inv.setItem(i, filler);
        }

        inv.setItem(SLOT_PREV_PAGE, page > 0 ? navButton(false) : filler);

        ItemStack randomBtn = buildRandomButton();
        for (int i = SLOT_RANDOM_START; i <= SLOT_RANDOM_END; i++) {
            inv.setItem(i, randomBtn);
        }

        inv.setItem(SLOT_NEXT_PAGE, page < totalPages - 1 ? navButton(true) : filler);

        return inv;
    }

    // ── Icônes ─────────────────────────────────────────────────────────────────

    private ItemStack buildArenaItem(Arena arena) {
        int max = Math.max(arena.getMaxPlayers(), 0);
        int current = arena.getCurrentPlayerCount();
        boolean running = arena.getState() == ArenaState.PLAYING || arena.getState() == ArenaState.SUDDEN_DEATH;
        boolean full = current >= max && max > 0;
        boolean joinable = arena.isSaved()
                && (arena.getState() == ArenaState.WAITING || arena.getState() == ArenaState.STARTING)
                && !full;

        Material mat;
        String displayName;
        String statusLine;

        if (!arena.isSaved() || arena.getState() == ArenaState.SETUP) {
            mat = Material.GRAY_STAINED_GLASS_PANE;
            displayName = ChatColor.GRAY + "" + ChatColor.BOLD + "✖ " + arena.getName();
            statusLine = ChatColor.GRAY + "Non configurée";
        } else if (running) {
            mat = Material.RED_STAINED_GLASS_PANE;
            displayName = ChatColor.RED + "" + ChatColor.BOLD + "⚔ " + arena.getName();
            statusLine = ChatColor.RED + "Partie en cours";
        } else if (full) {
            mat = Material.ORANGE_STAINED_GLASS_PANE;
            displayName = ChatColor.GOLD + "" + ChatColor.BOLD + "⚠ " + arena.getName();
            statusLine = ChatColor.GOLD + "Pleine";
        } else {
            mat = Material.LIME_STAINED_GLASS_PANE;
            displayName = ChatColor.GREEN + "" + ChatColor.BOLD + "✔ " + arena.getName();
            statusLine = ChatColor.GREEN + "Disponible";
        }

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Joueurs : " + ChatColor.WHITE + current + ChatColor.DARK_GRAY + "/" + ChatColor.GRAY + max);
        lore.add(ChatColor.GRAY + "Statut  : " + statusLine);
        lore.add("");
        if (joinable) {
            lore.add(ChatColor.YELLOW + "▶ Cliquez pour rejoindre !");
        } else if (running) {
            lore.add(ChatColor.AQUA + "\uD83D\uDC41 Cliquez pour regarder en spectateur !");
        } else {
            lore.add(ChatColor.RED + "✖ Indisponible");
        }

        return pane(mat, displayName, lore);
    }

    private ItemStack buildRandomButton() {
        long joinableCount = plugin.getArenaManager().getArenas().values().stream()
                .filter(a -> a.isSaved() && a.getState() != ArenaState.SETUP
                        && (a.getState() == ArenaState.WAITING || a.getState() == ArenaState.STARTING)
                        && !(a.getCurrentPlayerCount() >= a.getMaxPlayers() && a.getMaxPlayers() > 0))
                .count();

        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "✦ Rejoindre une arène aléatoire");
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Vous serez envoyé dans une arène");
        lore.add(ChatColor.GRAY + "disponible, en priorité celles");
        lore.add(ChatColor.GRAY + "qui ont déjà des joueurs.");
        lore.add("");
        if (joinableCount > 0) {
            lore.add(ChatColor.GREEN + "" + joinableCount + " arène(s) disponible(s)");
            lore.add("");
            lore.add(ChatColor.YELLOW + "▶ Cliquez pour jouer !");
        } else {
            lore.add(ChatColor.RED + "Aucune arène disponible pour le moment.");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack navButton(boolean next) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(next
                ? ChatColor.YELLOW + "" + ChatColor.BOLD + "Page suivante ▶"
                : ChatColor.YELLOW + "" + ChatColor.BOLD + "◀ Page précédente");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack pane(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ── Résolution des clics ──────────────────────────────────────────────────

    /** Meilleure arène pour un "join" aléatoire : joignable, en priorité celles qui ont déjà des joueurs. */
    public Arena findBestArenaForRandomJoin() {
        Arena best = null;
        for (Arena arena : plugin.getArenaManager().getArenas().values()) {
            if (!arena.isSaved() || arena.getState() == ArenaState.SETUP) continue;
            if (arena.getState() != ArenaState.WAITING && arena.getState() != ArenaState.STARTING) continue;
            if (arena.getMaxPlayers() > 0 && arena.getCurrentPlayerCount() >= arena.getMaxPlayers()) continue;
            if (best == null || arena.getCurrentPlayerCount() > best.getCurrentPlayerCount()) {
                best = arena;
            }
        }
        return best;
    }

    public Arena getArenaNameAt(int page, int slot) {
        if (slot < 0 || slot >= PAGE_SIZE) return null;
        return computePlacement().getOrDefault(page, Collections.emptyMap()).get(slot);
    }

    public static boolean isRandomButton(int slot) {
        return slot >= SLOT_RANDOM_START && slot <= SLOT_RANDOM_END;
    }

    public static boolean isPrevPageButton(int slot) {
        return slot == SLOT_PREV_PAGE;
    }

    public static boolean isNextPageButton(int slot) {
        return slot == SLOT_NEXT_PAGE;
    }

    public static boolean isArenaGuiTitle(String title) {
        return title != null && title.startsWith(TITLE_BASE);
    }

    public static int parsePageFromTitle(String title) {
        if (title == null) return 0;
        Matcher matcher = PAGE_TITLE_PATTERN.matcher(title);
        if (!matcher.find()) return 0;
        try {
            return Math.max(0, Integer.parseInt(matcher.group(1)) - 1);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static class AdminGUIHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
