package com.bedwars.shop;

import com.bedwars.BedwarsPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Charge/sauvegarde le contenu du shop (catégories + articles), entièrement configurable
 * en jeu via : /bd shop <catégorie> <slot> <item> <quantité> <prix> <minerai>
 *
 * Remplace l'ancien système figé en dur dans le code (ShopCategory) : plus besoin de
 * recompiler pour ajouter/modifier un article, tout est stocké dans shop.yml.
 *
 * Le nom de catégorie "tools" est réservé à l'onglet spécial pioche/hache à paliers
 * (voir {@link ToolTier}) et ne peut pas être utilisé ici.
 */
public class ShopConfigManager {

    public static final String RESERVED_TOOLS_CATEGORY = "tools";

    private final BedwarsPlugin plugin;
    private final File file;

    /** Ordre d'affichage des onglets (catégories), tel que découvert/ajouté par les admins. */
    private final List<String> categoryOrder = new java.util.ArrayList<>();
    /** catégorie -> (slot -> article). */
    private final Map<String, TreeMap<Integer, ShopItem>> items = new LinkedHashMap<>();

    public ShopConfigManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "shop.yml");
    }

    public void load() {
        categoryOrder.clear();
        items.clear();

        if (!file.exists()) {
            seedDefaults();
            save();
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        categoryOrder.addAll(config.getStringList("categories"));

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String category : itemsSection.getKeys(false)) {
                ConfigurationSection catSection = itemsSection.getConfigurationSection(category);
                if (catSection == null) continue;
                TreeMap<Integer, ShopItem> slots = new TreeMap<>();
                for (String slotKey : catSection.getKeys(false)) {
                    ConfigurationSection itemSection = catSection.getConfigurationSection(slotKey);
                    if (itemSection == null) continue;
                    try {
                        int slot = Integer.parseInt(slotKey);
                        Material material = Material.valueOf(itemSection.getString("material"));
                        Material currency = Material.valueOf(itemSection.getString("currency"));
                        int amount = itemSection.getInt("amount", 1);
                        int price = itemSection.getInt("price", 1);
                        slots.put(slot, new ShopItem(material, amount, currency, price));
                    } catch (Exception e) {
                        plugin.getLogger().warning("Article de shop invalide (" + category + "." + slotKey + "): " + e.getMessage());
                    }
                }
                if (!slots.isEmpty()) items.put(category, slots);
                if (!categoryOrder.contains(category)) categoryOrder.add(category);
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("categories", categoryOrder);
        for (Map.Entry<String, TreeMap<Integer, ShopItem>> catEntry : items.entrySet()) {
            String base = "items." + catEntry.getKey();
            for (Map.Entry<Integer, ShopItem> slotEntry : catEntry.getValue().entrySet()) {
                String slotBase = base + "." + slotEntry.getKey();
                ShopItem item = slotEntry.getValue();
                config.set(slotBase + ".material", item.getMaterial().name());
                config.set(slotBase + ".amount", item.getAmount());
                config.set(slotBase + ".price", item.getPrice());
                config.set(slotBase + ".currency", item.getCurrency().name());
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder shop.yml : " + e.getMessage());
        }
    }

    /** Ajoute/remplace un article à un slot précis d'une catégorie (créée si besoin). */
    public void setItem(String category, int slot, ShopItem item) {
        String key = category.toLowerCase();
        if (!categoryOrder.contains(key)) categoryOrder.add(key);
        items.computeIfAbsent(key, k -> new TreeMap<>()).put(slot, item);
        save();
    }

    public List<String> getCategories() {
        return categoryOrder;
    }

    public Map<Integer, ShopItem> getItems(String category) {
        return items.getOrDefault(category.toLowerCase(), new TreeMap<>());
    }

    public boolean categoryExists(String category) {
        return categoryOrder.contains(category.toLowerCase());
    }

    /** Remplit un jeu d'articles de départ raisonnable au tout premier démarrage (aucun shop.yml présent). */
    private void seedDefaults() {
        int slot;

        slot = 9;
        setItemSilent("blocks", slot++, new ShopItem(Material.WHITE_WOOL, 16, Material.IRON_INGOT, 4));
        setItemSilent("blocks", slot++, new ShopItem(Material.WHITE_TERRACOTTA, 16, Material.IRON_INGOT, 12));
        setItemSilent("blocks", slot++, new ShopItem(Material.GLASS, 16, Material.IRON_INGOT, 12));
        setItemSilent("blocks", slot++, new ShopItem(Material.END_STONE, 12, Material.IRON_INGOT, 24));
        setItemSilent("blocks", slot++, new ShopItem(Material.LADDER, 8, Material.IRON_INGOT, 4));
        setItemSilent("blocks", slot++, new ShopItem(Material.OAK_PLANKS, 16, Material.IRON_INGOT, 8));
        setItemSilent("blocks", slot, new ShopItem(Material.OBSIDIAN, 4, Material.EMERALD, 4));

        // Les épées ne sont plus vendues ici : elles sont gérées exclusivement via l'onglet
        // spécial "Tools" (paliers bois/pierre/fer/diamant, verrouillées au slot 1 de la hotbar).

        slot = 9;
        setItemSilent("armor", slot++, new ShopItem(Material.CHAINMAIL_BOOTS, 1, Material.IRON_INGOT, 40));
        setItemSilent("armor", slot++, new ShopItem(Material.IRON_BOOTS, 1, Material.GOLD_INGOT, 12));
        setItemSilent("armor", slot, new ShopItem(Material.DIAMOND_BOOTS, 1, Material.EMERALD, 8));

        slot = 9;
        setItemSilent("ranged", slot++, new ShopItem(Material.ARROW, 6, Material.GOLD_INGOT, 2));
        setItemSilent("ranged", slot, new ShopItem(Material.BOW, 1, Material.GOLD_INGOT, 12));

        slot = 9;
        setItemSilent("potions", slot++, new ShopItem(Material.POTION, 1, Material.EMERALD, 1));
        setItemSilent("potions", slot, new ShopItem(Material.POTION, 1, Material.EMERALD, 2));

        slot = 9;
        setItemSilent("utility", slot++, new ShopItem(Material.GOLDEN_APPLE, 1, Material.GOLD_INGOT, 3));
        setItemSilent("utility", slot++, new ShopItem(Material.ENDER_PEARL, 1, Material.EMERALD, 4));
        setItemSilent("utility", slot++, new ShopItem(Material.WATER_BUCKET, 1, Material.GOLD_INGOT, 2));
        setItemSilent("utility", slot, new ShopItem(Material.TNT, 1, Material.GOLD_INGOT, 4));
    }

    private void setItemSilent(String category, int slot, ShopItem item) {
        String key = category.toLowerCase();
        if (!categoryOrder.contains(key)) categoryOrder.add(key);
        items.computeIfAbsent(key, k -> new TreeMap<>()).put(slot, item);
    }
}
