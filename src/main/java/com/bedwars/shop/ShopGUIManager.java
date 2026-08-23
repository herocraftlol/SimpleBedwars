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
    private static final String QUICKBUY_CATEGORY = "quickbuy";
    private static final int QUICKBUY_TAB_SLOT = 0;
    private static final int TOOLS_TAB_SLOT = 8;
    private static final int SLOT_PICKAXE = 20;
    private static final int SLOT_AXE = 24;
    private static final int SLOT_SWORD = 22;

    private final BedwarsPlugin plugin;

    public ShopGUIManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Arena arena, TeamColor team) {
        // "Quick Buy" (favoris) est l'onglet affiché par défaut à l'ouverture, comme sur Hypixel.
        ShopHolder holder = new ShopHolder(arena.getName(), team, QUICKBUY_CATEGORY);
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

        // Rangée du haut : Quick Buy (favoris, en haut à gauche) - catégories configurées (jusqu'à
        // 7, slots 1-7) - Tools (slot 8, toujours en dernier).
        boolean quickBuySelected = QUICKBUY_CATEGORY.equalsIgnoreCase(holder.getCategory());
        ItemStack quickBuyTab = pane(Material.NETHER_STAR,
                (quickBuySelected ? ChatColor.YELLOW + "" + ChatColor.BOLD : ChatColor.GREEN + "") + "Quick Buy",
                quickBuySelected
                        ? List.of(ChatColor.GRAY + "» Vos achats favoris «")
                        : List.of(ChatColor.YELLOW + "Cliquez pour voir vos favoris !",
                                  ChatColor.DARK_GRAY + "(Shift-clic sur un article pour l'ajouter ici)"));
        inv.setItem(QUICKBUY_TAB_SLOT, quickBuyTab);

        List<String> categories = plugin.getShopConfigManager().getCategories();
        int tabSlot = 1;
        for (String category : categories) {
            if (tabSlot >= TOOLS_TAB_SLOT) break; // slot 8 réservé à Tools
            boolean selected = category.equalsIgnoreCase(holder.getCategory());
            inv.setItem(tabSlot, categoryTab(category, selected));
            tabSlot++;
        }
        boolean toolsSelected = ShopConfigManager.RESERVED_TOOLS_CATEGORY.equalsIgnoreCase(holder.getCategory());
        inv.setItem(TOOLS_TAB_SLOT, categoryTab("Tools", toolsSelected));

        holder.getSlotItems().clear();
        holder.getSlotRefs().clear();

        if (quickBuySelected) {
            populateQuickBuy(player, holder, inv);
        } else if (ShopConfigManager.RESERVED_TOOLS_CATEGORY.equalsIgnoreCase(holder.getCategory())) {
            populateTools(player, holder, inv);
        } else {
            populateGenericItems(player, holder, inv);
        }

        inv.setItem(CLOSE_SLOT, pane(Material.BARRIER, ChatColor.RED + "Fermer", List.of()));
    }

    /** Onglet Quick Buy : les articles favoris du joueur (voir FavoritesManager), rangés proprement. */
    private void populateQuickBuy(Player player, ShopHolder holder, Inventory inv) {
        List<String> favs = plugin.getFavoritesManager().getFavorites(player.getUniqueId());
        if (favs.isEmpty()) {
            inv.setItem(31, pane(Material.NETHER_STAR, ChatColor.YELLOW + "" + ChatColor.BOLD + "Aucun favori pour l'instant",
                    List.of(ChatColor.GRAY + "Allez dans une autre catégorie et faites",
                            ChatColor.GRAY + "Shift + clic sur un article pour l'ajouter ici.")));
            return;
        }
        int slot = 9;
        for (String ref : favs) {
            if (slot >= 45) break;
            ShopItem item = resolveRef(ref);
            if (item == null) continue; // référence obsolète (article supprimé depuis) : ignorée
            inv.setItem(slot, buildItemStack(holder.getTeam(), item, true));
            holder.getSlotItems().put(slot, item);
            holder.getSlotRefs().put(slot, ref);
            slot++;
        }
    }

    /** Retrouve un ShopItem à partir de sa référence stable (voir FavoritesManager). */
    private ShopItem resolveRef(String ref) {
        int sep = ref.indexOf(':');
        if (sep < 0) return null;
        String prefix = ref.substring(0, sep);
        String rest = ref.substring(sep + 1);
        try {
            if (prefix.equals("special_potions")) {
                int idx = Integer.parseInt(rest);
                return idx >= 0 && idx < SpecialShopItems.POTIONS.size() ? SpecialShopItems.POTIONS.get(idx) : null;
            }
            if (prefix.equals("special_ranged")) {
                int idx = Integer.parseInt(rest);
                return idx >= 0 && idx < SpecialShopItems.RANGED_EXTRA.size() ? SpecialShopItems.RANGED_EXTRA.get(idx) : null;
            }
            int configSlot = Integer.parseInt(rest);
            return plugin.getShopConfigManager().getItems(prefix).get(configSlot);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void populateGenericItems(Player player, ShopHolder holder, Inventory inv) {
        Map<Integer, ShopItem> items = plugin.getShopConfigManager().getItems(holder.getCategory());
        int slot = 9;
        for (Map.Entry<Integer, ShopItem> entry : items.entrySet()) {
            if (slot >= 45) break;
            String ref = holder.getCategory().toLowerCase() + ":" + entry.getKey();
            boolean fav = plugin.getFavoritesManager().isFavorite(player.getUniqueId(), ref);
            inv.setItem(slot, buildItemStack(holder.getTeam(), entry.getValue(), fav));
            holder.getSlotItems().put(slot, entry.getValue());
            holder.getSlotRefs().put(slot, ref);
            slot++;
        }

        List<ShopItem> special = switch (holder.getCategory().toLowerCase()) {
            case "potions" -> SpecialShopItems.POTIONS;
            case "ranged" -> SpecialShopItems.RANGED_EXTRA;
            default -> List.of();
        };
        String specialPrefix = holder.getCategory().equalsIgnoreCase("potions") ? "special_potions" : "special_ranged";
        int idx = 0;
        for (ShopItem item : special) {
            if (slot >= 45) break;
            String ref = specialPrefix + ":" + idx;
            boolean fav = plugin.getFavoritesManager().isFavorite(player.getUniqueId(), ref);
            inv.setItem(slot, buildItemStack(holder.getTeam(), item, fav));
            holder.getSlotItems().put(slot, item);
            holder.getSlotRefs().put(slot, ref);
            slot++;
            idx++;
        }

        if (items.isEmpty() && special.isEmpty()) {
            inv.setItem(22, pane(Material.PAPER, ChatColor.GRAY + "Catégorie vide",
                    List.of(ChatColor.DARK_GRAY + "Aucun article configuré ici.",
                            ChatColor.DARK_GRAY + "/bd shop " + holder.getCategory() + " <slot> <item> <qté> <prix> <minerai>")));
        }
    }

    private void populateTools(Player player, ShopHolder holder, Inventory inv) {
        GameInstance instance = plugin.getGameManager().getInstance(
                plugin.getArenaManager().getArena(holder.getArenaName()));
        int pLevel = instance.getPickaxeTier(player.getUniqueId());
        int aLevel = instance.getAxeTier(player.getUniqueId());
        SwordTier sTier = SwordTier.byLevel(instance.getSwordTier(player.getUniqueId()));

        inv.setItem(SLOT_PICKAXE, toolUpgradeItem("Pioche", pLevel));
        inv.setItem(SLOT_AXE, toolUpgradeItem("Hache", aLevel));
        inv.setItem(SLOT_SWORD, swordUpgradeItem(sTier));
    }

    private ItemStack swordUpgradeItem(SwordTier current) {
        SwordTier next = current.next();
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Palier actuel: " + ChatColor.YELLOW + swordTierLabel(current));
        lore.add("");
        if (current == next) {
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "PALIER MAXIMUM");
        } else {
            lore.add(ChatColor.GOLD + "Prix: " + ChatColor.WHITE + next.getPrice() + " " + currencyLabel(next.getCurrency()));
            lore.add(ChatColor.YELLOW + "Cliquez pour passer au palier " + swordTierLabel(next) + " !");
        }
        return pane(current.getMaterial(), ChatColor.AQUA + "" + ChatColor.BOLD + "Épée", lore);
    }

    private String swordTierLabel(SwordTier tier) {
        return switch (tier) {
            case WOOD -> "Bois";
            case STONE -> "Pierre";
            case IRON -> "Fer";
            case DIAMOND -> "Diamant";
        };
    }

    /** level = -1 signifie "pas encore achetée" : le palier bois (payant) sert alors de premier achat. */
    private ItemStack toolUpgradeItem(String name, int level) {
        List<String> lore = new ArrayList<>();
        if (level < 0) {
            ToolTier first = ToolTier.WOOD;
            Material icon = name.equals("Pioche") ? first.getPickaxe() : first.getAxe();
            lore.add(ChatColor.GRAY + "Vous n'avez pas encore de " + name.toLowerCase() + ".");
            lore.add("");
            lore.add(ChatColor.GOLD + "Prix: " + ChatColor.WHITE + first.getPrice() + " " + currencyLabel(first.getCurrency()));
            lore.add(ChatColor.YELLOW + "Cliquez pour l'acheter !");
            return pane(icon, ChatColor.AQUA + "" + ChatColor.BOLD + name, lore);
        }

        ToolTier current = ToolTier.byLevel(level);
        ToolTier next = current.next();
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
            case STONE -> "Pierre";
            case IRON -> "Fer";
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
        return buildItemStack(team, item, false);
    }

    private ItemStack buildItemStack(TeamColor team, ShopItem item, boolean favorite) {
        if (item.getPotionType() != null) {
            return buildPotionStack(item, favorite);
        }

        Material material = TeamColorUtil.colorize(item.getMaterial(), team);
        ItemStack stack = new ItemStack(material, 1);
        for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> ench : item.getEnchantments().entrySet()) {
            stack.addUnsafeEnchantment(ench.getKey(), ench.getValue());
        }
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName((favorite ? ChatColor.YELLOW + "★ " : ChatColor.WHITE + "") + item.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GOLD + "Prix: " + ChatColor.WHITE + item.getAmount() + "x pour " + item.getPrice() + " " + item.getCurrencyName());
        lore.add(ChatColor.YELLOW + "Cliquez pour acheter !");
        lore.add(favorite ? ChatColor.GOLD + "Shift-clic pour retirer des favoris" : ChatColor.DARK_GRAY + "Shift-clic pour ajouter aux favoris");
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack buildPotionStack(ShopItem item, boolean favorite) {
        ItemStack stack = new ItemStack(item.getMaterial(), 1);
        org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) stack.getItemMeta();
        meta.setBasePotionType(item.getPotionType());
        meta.setDisplayName((favorite ? ChatColor.YELLOW + "★ " : ChatColor.WHITE + "") + item.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GOLD + "Prix: " + ChatColor.WHITE + item.getPrice() + " " + item.getCurrencyName());
        lore.add(ChatColor.YELLOW + "Cliquez pour acheter !");
        lore.add(favorite ? ChatColor.GOLD + "Shift-clic pour retirer des favoris" : ChatColor.DARK_GRAY + "Shift-clic pour ajouter aux favoris");
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    /** Gère un clic dans le GUI du shop. Retourne true si le clic a été traité ici. */
    public boolean handleClick(Player player, Inventory topInventory, int slot, boolean isShiftClick) {
        if (!(topInventory.getHolder() instanceof ShopHolder holder)) return false;
        Arena arena = plugin.getArenaManager().getArena(holder.getArenaName());
        if (arena == null) return true;

        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return true;
        }

        if (slot == QUICKBUY_TAB_SLOT) {
            holder.setCategory(QUICKBUY_CATEGORY);
            populate(player, arena, topInventory, holder);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return true;
        }
        if (slot == TOOLS_TAB_SLOT) {
            holder.setCategory(ShopConfigManager.RESERVED_TOOLS_CATEGORY);
            populate(player, arena, topInventory, holder);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return true;
        }
        List<String> categories = plugin.getShopConfigManager().getCategories();
        int categoryIndex = slot - 1; // slot 1 = 1ère catégorie configurée, slot 0 = Quick Buy
        if (slot > QUICKBUY_TAB_SLOT && slot < TOOLS_TAB_SLOT && categoryIndex < categories.size()) {
            holder.setCategory(categories.get(categoryIndex));
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

        if (isShiftClick) {
            String ref = holder.getSlotRefs().get(slot);
            if (ref != null) {
                boolean added = plugin.getFavoritesManager().toggle(player.getUniqueId(), ref);
                player.sendMessage(ChatColor.GREEN + (added ? "Ajouté aux favoris (Quick Buy) !" : "Retiré des favoris."));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                populate(player, arena, topInventory, holder);
            }
            return true;
        }

        purchase(player, holder, item, topInventory, arena);
        return true;
    }

    private void handleToolPurchase(Player player, Arena arena, ShopHolder holder, Inventory topInventory, int slot) {
        GameInstance instance = plugin.getGameManager().getInstance(arena);

        if (slot == SLOT_SWORD) {
            SwordTier current = SwordTier.byLevel(instance.getSwordTier(player.getUniqueId()));
            SwordTier next = current.next();
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
            instance.setSwordTier(player.getUniqueId(), next.getLevel());
            instance.refreshSwordItem(player);
            player.sendMessage(ChatColor.GREEN + "Palier amélioré: " + ChatColor.WHITE + swordTierLabel(next));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            populate(player, arena, topInventory, holder);
            return;
        }

        if (slot != SLOT_PICKAXE && slot != SLOT_AXE) return;
        boolean isPickaxe = slot == SLOT_PICKAXE;
        int currentLevel = isPickaxe ? instance.getPickaxeTier(player.getUniqueId()) : instance.getAxeTier(player.getUniqueId());

        // Palier suivant : si jamais achetée (-1), le tout premier achat est le palier bois
        // (payant, comme les autres) ; sinon le palier suivant celui déjà possédé.
        ToolTier next = currentLevel < 0 ? ToolTier.WOOD : ToolTier.byLevel(currentLevel).next();
        boolean alreadyMaxed = currentLevel >= 0 && ToolTier.byLevel(currentLevel) == next;
        if (alreadyMaxed) {
            player.sendMessage(ChatColor.RED + "Palier déjà maximum.");
            return;
        }
        if (!EconomyUtil.hasCurrency(player, next.getCurrency(), next.getPrice())) {
            player.sendMessage(ChatColor.RED + "Il vous manque des " + currencyLabel(next.getCurrency()) + " pour ce palier.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        EconomyUtil.removeCurrency(player, next.getCurrency(), next.getPrice());

        String prefKey = isPickaxe ? com.bedwars.util.PlayerPrefsManager.PICKAXE : com.bedwars.util.PlayerPrefsManager.AXE;
        int defaultSlot = isPickaxe ? com.bedwars.util.KitProtectionUtil.SLOT_PICKAXE : com.bedwars.util.KitProtectionUtil.SLOT_AXE;
        Material newMaterial = isPickaxe ? next.getPickaxe() : next.getAxe();
        String label = isPickaxe ? "Pioche" : "Hache";
        ItemStack newTool = com.bedwars.util.KitProtectionUtil.tagAsKitTool(new ItemStack(newMaterial), label);

        int existingSlot = instance.findKitToolSlot(player, isPickaxe
                ? java.util.Set.of(Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE)
                : java.util.Set.of(Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.DIAMOND_AXE));
        if (existingSlot >= 0) {
            player.getInventory().setItem(existingSlot, newTool);
        } else {
            int preferredSlot = plugin.getPlayerPrefsManager().getSlot(player.getUniqueId(), prefKey, defaultSlot);
            instance.placeAtPreferredSlot(player, preferredSlot, newTool);
        }

        if (isPickaxe) {
            instance.setPickaxeTier(player.getUniqueId(), next.getLevel());
        } else {
            instance.setAxeTier(player.getUniqueId(), next.getLevel());
        }

        String verb = currentLevel < 0 ? "Achetée: " : "Palier amélioré: ";
        player.sendMessage(ChatColor.GREEN + verb + ChatColor.WHITE + label + " " + tierLabel(next)
                + (currentLevel < 0 ? ChatColor.GRAY + " (vous la garderez pour le reste de la partie)" : ""));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        populate(player, arena, topInventory, holder);
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

        if (item.getPotionType() != null) {
            giveOrDrop(player, buildPurchasedPotion(item));
        } else {
            Material material = TeamColorUtil.colorize(item.getMaterial(), holder.getTeam());
            if (!equipIfArmorPiece(player, material)) {
                ItemStack stack = new ItemStack(material, item.getAmount());
                for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> ench : item.getEnchantments().entrySet()) {
                    stack.addUnsafeEnchantment(ench.getKey(), ench.getValue());
                }
                giveOrDrop(player, stack);
            }
        }

        player.sendMessage(ChatColor.GREEN + "Acheté: " + ChatColor.WHITE + item.getAmount() + "x " + item.getDisplayName());
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        populate(player, arena, topInventory, holder);
    }

    private void giveOrDrop(Player player, ItemStack stack) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        for (ItemStack extra : leftover.values()) {
            player.getWorld().dropItem(player.getLocation(), extra);
        }
    }

    /** Construit la potion jetable réellement donnée au joueur, avec la durée exacte configurée. */
    private ItemStack buildPurchasedPotion(ShopItem item) {
        ItemStack stack = new ItemStack(item.getMaterial(), 1);
        org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) stack.getItemMeta();
        meta.setBasePotionType(item.getPotionType());
        meta.setDisplayName(ChatColor.WHITE + item.getDisplayName());

        org.bukkit.potion.PotionEffectType effectType = switch (item.getPotionType()) {
            case STRENGTH -> org.bukkit.potion.PotionEffectType.STRENGTH;
            case SWIFTNESS -> org.bukkit.potion.PotionEffectType.SPEED;
            case INVISIBILITY -> org.bukkit.potion.PotionEffectType.INVISIBILITY;
            case LEAPING -> org.bukkit.potion.PotionEffectType.JUMP_BOOST;
            case HEALING -> org.bukkit.potion.PotionEffectType.INSTANT_HEALTH;
            default -> null;
        };
        if (effectType != null && item.getPotionDurationTicks() > 0) {
            meta.addCustomEffect(new org.bukkit.potion.PotionEffect(effectType, item.getPotionDurationTicks(), 0), true);
        }
        stack.setItemMeta(meta);
        return stack;
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
        private final Map<Integer, String> slotRefs = new java.util.HashMap<>();
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

        public Map<Integer, String> getSlotRefs() {
            return slotRefs;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
