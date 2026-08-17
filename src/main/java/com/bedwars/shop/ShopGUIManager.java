package com.bedwars.shop;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.TeamColor;
import com.bedwars.game.GameInstance;
import com.bedwars.util.EconomyUtil;
import com.bedwars.util.TeamColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Construit et gère l'interface du shop d'équipe (PNJ "Marchand").
 *
 * Les onglets/articles "normaux" sont entièrement définis en jeu (voir {@link ShopConfigManager}
 * et la commande /bd shop <catégorie> <slot> <item> <quantité> <prix> <minerai>) : plus besoin
 * de recompiler pour changer le contenu du shop.
 *
 * Un onglet spécial "Tools" est en revanche géré à part (voir {@link ToolTier}) : pioche et
 * hache à paliers bois/fer/or/diamant, qui redescendent d'un palier à chaque mort (le palier
 * bois, lui, reste acquis pour toujours).
 *
 * Les blocs "colorés" (laine, terre cuite, béton, verre teinté, tapis...) configurés dans le
 * shop sont automatiquement recolorés à la couleur de l'équipe de l'acheteur, aussi bien dans
 * l'aperçu du GUI qu'au moment de l'achat (voir {@link TeamColorUtil}).
 */
public class ShopGUIManager {

    private static final int SIZE = 54;
    private static final int CLOSE_SLOT = 49;
    private static final int TOOLS_TAB_SLOT = 8;
    private static final int SLOT_PICKAXE = 20;
    private static final int SLOT_AXE = 24;

    private final BedwarsPlugin plugin;

    public ShopGUIManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Arena arena, TeamColor team) {
        List<String> categories = plugin.getShopConfigManager().getCategories();
        String defaultCategory = categories.isEmpty() ? ShopConfigManager.RESERVED_TOOLS_CATEGORY : categories.get(0);

        ShopHolder holder = new ShopHolder(arena.getName(), team, defaultCategory);
        Inventory inv = Bukkit.createInventory(holder, SIZE,
                ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Marchand " + ChatColor.RESET
                        + ChatColor.GRAY + "- " + team.getColoredName());
        holder.setInventory(inv);
        populate(player, arena, inv, holder);
        player.openInventory(inv);
    }

    private void populate(Player player, Arena arena, Inventory inv, ShopHolder holder) {
        inv.clear();
        ItemStack filler = pane(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < SIZE; i++) inv.setItem(i, filler);

        // Rangée du haut : les catégories configurées (jusqu'à 8) + l'onglet spécial "Tools".
        List<String> categories = plugin.getShopConfigManager().getCategories();
        int tabSlot = 0;
        for (String category : categories) {
            if (tabSlot >= TOOLS_TAB_SLOT) break; // slot 8 réservé à Tools
            boolean selected = category.equalsIgnoreCase(holder.getCategory());
            inv.setItem(tabSlot, categoryTab(category, selected));
            tabSlot++;
        }
        boolean toolsSelected = ShopConfigManager.RESERVED_TOOLS_CATEGORY.equalsIgnoreCase(holder.getCategory());
        inv.setItem(TOOLS_TAB_SLOT, categoryTab("Tools", toolsSelected));

        holder.getSlotItems().clear();

        if (ShopConfigManager.RESERVED_TOOLS_CATEGORY.equalsIgnoreCase(holder.getCategory())) {
            populateTools(player, holder, inv);
        } else {
            populateGenericItems(player, holder, inv);
        }

        inv.setItem(CLOSE_SLOT, pane(Material.BARRIER, ChatColor.RED + "Fermer", List.of()));
    }

    private void populateGenericItems(Player player, ShopHolder holder, Inventory inv) {
        Map<Integer, ShopItem> items = plugin.getShopConfigManager().getItems(holder.getCategory());
        int slot = 9;
        for (ShopItem item : items.values()) {
            if (slot >= 45) break;
            inv.setItem(slot, buildItemStack(holder.getTeam(), item));
            holder.getSlotItems().put(slot, item);
            slot++;
        }
        if (items.isEmpty()) {
            inv.setItem(22, pane(Material.PAPER, ChatColor.GRAY + "Catégorie vide",
                    List.of(ChatColor.DARK_GRAY + "Aucun article configuré ici.",
                            ChatColor.DARK_GRAY + "/bd shop " + holder.getCategory() + " <slot> <item> <qté> <prix> <minerai>")));
        }
    }

    private void populateTools(Player player, ShopHolder holder, Inventory inv) {
        GameInstance instance = plugin.getGameManager().getInstance(
                plugin.getArenaManager().getArena(holder.getArenaName()));
        ToolTier pTier = ToolTier.byLevel(instance.getPickaxeTier(player.getUniqueId()));
        ToolTier aTier = ToolTier.byLevel(instance.getAxeTier(player.getUniqueId()));

        inv.setItem(SLOT_PICKAXE, toolUpgradeItem("Pioche", pTier));
        inv.setItem(SLOT_AXE, toolUpgradeItem("Hache", aTier));
    }

    private ItemStack toolUpgradeItem(String name, ToolTier current) {
        ToolTier next = current.next();
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Palier actuel: " + ChatColor.YELLOW + tierLabel(current));
        lore.add("");
        if (current == next) {
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "PALIER MAXIMUM");
        } else {
            lore.add(ChatColor.GOLD + "Prix: " + ChatColor.WHITE + next.getPrice() + " " + currencyLabel(next.getCurrency()));
            lore.add(ChatColor.YELLOW + "Cliquez pour passer au palier " + tierLabel(next) + " !");
        }
        Material icon = name.equals("Pioche") ? current.getPickaxe() : current.getAxe();
        return pane(icon, ChatColor.AQUA + "" + ChatColor.BOLD + name, lore);
    }

    private String tierLabel(ToolTier tier) {
        return switch (tier) {
            case WOOD -> "Bois";
            case IRON -> "Fer";
            case GOLD -> "Or";
            case DIAMOND -> "Diamant";
        };
    }

    private String currencyLabel(Material currency) {
        if (currency == null) return "";
        return switch (currency) {
            case IRON_INGOT -> "Fer";
            case GOLD_INGOT -> "Or";
            case EMERALD -> "Émeraude(s)";
            case DIAMOND -> "Diamant(s)";
            default -> currency.name();
        };
    }

    private ItemStack categoryTab(String category, boolean selected) {
        String label = Character.toUpperCase(category.charAt(0)) + category.substring(1).toLowerCase();
        String name = (selected ? ChatColor.YELLOW + "" + ChatColor.BOLD : ChatColor.GREEN + "") + label;
        List<String> lore = selected
                ? List.of(ChatColor.GRAY + "» Catégorie actuelle «")
                : List.of(ChatColor.YELLOW + "Cliquez pour voir !");
        Material icon = switch (category.toLowerCase()) {
            case "blocks" -> Material.TERRACOTTA;
            case "melee" -> Material.GOLDEN_SWORD;
            case "armor" -> Material.CHAINMAIL_BOOTS;
            case "ranged" -> Material.BOW;
            case "potions" -> Material.BREWING_STAND;
            case "utility" -> Material.TNT;
            case "tools" -> Material.STONE_PICKAXE;
            default -> Material.CHEST;
        };
        return pane(icon, name, lore);
    }

    private ItemStack buildItemStack(TeamColor team, ShopItem item) {
        Material material = TeamColorUtil.colorize(item.getMaterial(), team);
        ItemStack stack = new ItemStack(material, 1);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + item.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GOLD + "Prix: " + ChatColor.WHITE + item.getAmount() + "x pour " + item.getPrice() + " " + item.getCurrencyName());
        lore.add(ChatColor.YELLOW + "Cliquez pour acheter !");
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    /** Gère un clic dans le GUI du shop. Retourne true si le clic a été traité ici. */
    public boolean handleClick(Player player, Inventory topInventory, int slot) {
        if (!(topInventory.getHolder() instanceof ShopHolder holder)) return false;
        Arena arena = plugin.getArenaManager().getArena(holder.getArenaName());
        if (arena == null) return true;

        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return true;
        }

        if (slot == TOOLS_TAB_SLOT) {
            holder.setCategory(ShopConfigManager.RESERVED_TOOLS_CATEGORY);
            populate(player, arena, topInventory, holder);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return true;
        }
        List<String> categories = plugin.getShopConfigManager().getCategories();
        if (slot >= 0 && slot < TOOLS_TAB_SLOT && slot < categories.size()) {
            holder.setCategory(categories.get(slot));
            populate(player, arena, topInventory, holder);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return true;
        }

        if (ShopConfigManager.RESERVED_TOOLS_CATEGORY.equalsIgnoreCase(holder.getCategory())) {
            handleToolPurchase(player, arena, holder, topInventory, slot);
            return true;
        }

        ShopItem item = holder.getSlotItems().get(slot);
        if (item == null) return true;
        purchase(player, holder, item, topInventory, arena);
        return true;
    }

    private void handleToolPurchase(Player player, Arena arena, ShopHolder holder, Inventory topInventory, int slot) {
        if (slot != SLOT_PICKAXE && slot != SLOT_AXE) return;

        GameInstance instance = plugin.getGameManager().getInstance(arena);
        boolean isPickaxe = slot == SLOT_PICKAXE;
        int currentLevel = isPickaxe ? instance.getPickaxeTier(player.getUniqueId()) : instance.getAxeTier(player.getUniqueId());
        ToolTier current = ToolTier.byLevel(currentLevel);
        ToolTier next = current.next();

        if (next == current) {
            player.sendMessage(ChatColor.RED + "Palier déjà maximum.");
            return;
        }
        if (!EconomyUtil.hasCurrency(player, next.getCurrency(), next.getPrice())) {
            player.sendMessage(ChatColor.RED + "Il vous manque des " + currencyLabel(next.getCurrency()) + " pour ce palier.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        EconomyUtil.removeCurrency(player, next.getCurrency(), next.getPrice());

        if (isPickaxe) {
            instance.setPickaxeTier(player.getUniqueId(), next.getLevel());
            replaceTool(player, current.getPickaxe(), next.getPickaxe());
        } else {
            instance.setAxeTier(player.getUniqueId(), next.getLevel());
            replaceTool(player, current.getAxe(), next.getAxe());
        }

        player.sendMessage(ChatColor.GREEN + "Palier amélioré: " + ChatColor.WHITE + tierLabel(next));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        populate(player, arena, topInventory, holder);
    }

    private void replaceTool(Player player, Material oldTool, Material newTool) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == oldTool) {
                stack.setType(newTool);
                return;
            }
        }
        player.getInventory().addItem(new ItemStack(newTool));
    }

    private void purchase(Player player, ShopHolder holder, ShopItem item, Inventory topInventory, Arena arena) {
        if (!EconomyUtil.hasCurrency(player, item.getCurrency(), item.getPrice())) {
            int have = EconomyUtil.countCurrency(player, item.getCurrency());
            player.sendMessage(ChatColor.RED + "Il vous manque " + (item.getPrice() - have) + " "
                    + item.getCurrencyName() + " pour acheter " + item.getDisplayName() + ".");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        EconomyUtil.removeCurrency(player, item.getCurrency(), item.getPrice());

        Material material = TeamColorUtil.colorize(item.getMaterial(), holder.getTeam());

        if (!equipIfArmorPiece(player, material)) {
            ItemStack stack = new ItemStack(material, item.getAmount());
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            for (ItemStack extra : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), extra);
            }
        }

        player.sendMessage(ChatColor.GREEN + "Acheté: " + ChatColor.WHITE + item.getAmount() + "x " + item.getDisplayName());
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        populate(player, arena, topInventory, holder);
    }

    /** Équipe directement bottes/jambières/plastron/casque achetés, plutôt que de les mettre en vrac dans l'inventaire. */
    private boolean equipIfArmorPiece(Player player, Material material) {
        String name = material.name();
        if (name.endsWith("_BOOTS")) {
            player.getInventory().setBoots(new ItemStack(material));
        } else if (name.endsWith("_LEGGINGS")) {
            player.getInventory().setLeggings(new ItemStack(material));
        } else if (name.endsWith("_CHESTPLATE")) {
            player.getInventory().setChestplate(new ItemStack(material));
        } else if (name.endsWith("_HELMET")) {
            player.getInventory().setHelmet(new ItemStack(material));
        } else {
            return false;
        }
        return true;
    }

    public void onClose(Player player) {
        // Rien de spécial à nettoyer : l'inventaire est jetable, recréé à chaque ouverture.
    }

    private ItemStack pane(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /** Détient l'état d'un GUI de shop ouvert : arène, équipe, catégorie actuelle. */
    public static class ShopHolder implements InventoryHolder {
        private final String arenaName;
        private final TeamColor team;
        private String category;
        private final Map<Integer, ShopItem> slotItems = new java.util.HashMap<>();
        private Inventory inventory;

        public ShopHolder(String arenaName, TeamColor team, String category) {
            this.arenaName = arenaName;
            this.team = team;
            this.category = category;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        public String getArenaName() {
            return arenaName;
        }

        public TeamColor getTeam() {
            return team;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public Map<Integer, ShopItem> getSlotItems() {
            return slotItems;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
