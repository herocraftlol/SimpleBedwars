package com.bedwars.shop;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.GeneratorType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class ShopConfigManager {

    private final BedwarsPlugin plugin;
    private final File file;
    private ShopConfig config = new ShopConfig();

    public ShopConfigManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "shop_config.yml");
    }

    public ShopConfig getConfig() {
        return config;
    }

    public void load() {
        if (!file.exists()) {
            this.config = new ShopConfig();
            HypixelShopDefaults.apply(config);
            save();
            plugin.getLogger().info("Aucun shop_config.yml trouvé : génération d'un shop par défaut inspiré de Hypixel.");
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ShopConfig loaded = new ShopConfig();

        if (yml.isConfigurationSection("shopGuis")) {
            for (String guiName : Objects.requireNonNull(yml.getConfigurationSection("shopGuis")).getKeys(false)) {
                GuiDefinition gui = loaded.getOrCreateShopGui(guiName);
                ConfigurationSection slotsSection = yml.getConfigurationSection("shopGuis." + guiName);
                if (slotsSection == null) continue;
                for (String slotKey : slotsSection.getKeys(false)) {
                    int index;
                    try {
                        index = Integer.parseInt(slotKey);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    String base = "shopGuis." + guiName + "." + slotKey;
                    String kind = yml.getString(base + ".kind");
                    if ("SHOP_ITEM".equals(kind)) {
                        Material mat = Material.matchMaterial(Objects.requireNonNull(yml.getString(base + ".material")));
                        int price = yml.getInt(base + ".price");
                        int amount = yml.getInt(base + ".amount", 1);
                        GeneratorType currency = GeneratorType.valueOf(yml.getString(base + ".currency"));
                        gui.setSlot(index, GuiSlot.shopItem(mat, amount, price, currency));
                    } else if ("SHOP_LINK".equals(kind)) {
                        Material mat = Material.matchMaterial(Objects.requireNonNull(yml.getString(base + ".material")));
                        String linked = yml.getString(base + ".linkedGui");
                        gui.setSlot(index, GuiSlot.shopLink(mat, linked));
                    }
                }
            }
        }

        if (yml.isConfigurationSection("upgradeGui")) {
            ConfigurationSection slotsSection = yml.getConfigurationSection("upgradeGui");
            for (String slotKey : Objects.requireNonNull(slotsSection).getKeys(false)) {
                int index;
                try {
                    index = Integer.parseInt(slotKey);
                } catch (NumberFormatException e) {
                    continue;
                }
                String base = "upgradeGui." + slotKey;
                Material mat = Material.matchMaterial(Objects.requireNonNull(yml.getString(base + ".material")));
                UpgradeType type = UpgradeType.valueOf(Objects.requireNonNull(yml.getString(base + ".type")));
                int startLevel = yml.getInt(base + ".startLevel", 1);
                loaded.getUpgradeGui().setSlot(index, GuiSlot.upgradeItem(mat, type, startLevel));
            }
        }

        if (yml.isConfigurationSection("upgradeTypeConfigs")) {
            for (String typeName : Objects.requireNonNull(yml.getConfigurationSection("upgradeTypeConfigs")).getKeys(false)) {
                UpgradeType type;
                try {
                    type = UpgradeType.valueOf(typeName);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                UpgradeTypeConfig cfg = loaded.getUpgradeConfig(type);
                cfg.setMaxLevel(yml.getInt("upgradeTypeConfigs." + typeName + ".maxLevel", type.getDefaultMaxLevel()));
                ConfigurationSection prices = yml.getConfigurationSection("upgradeTypeConfigs." + typeName + ".prices");
                if (prices != null) {
                    for (String lvl : prices.getKeys(false)) {
                        try {
                            cfg.setPriceForLevel(Integer.parseInt(lvl), prices.getInt(lvl));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }

        this.config = loaded;
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();

        for (GuiDefinition gui : config.getShopGuis().values()) {
            for (var entry : gui.getSlots().entrySet()) {
                GuiSlot slot = entry.getValue();
                String base = "shopGuis." + gui.getName() + "." + entry.getKey();
                yml.set(base + ".kind", slot.getKind().name());
                if (slot.getMaterial() != null) yml.set(base + ".material", slot.getMaterial().name());
                if (slot.getKind() == GuiSlot.Kind.SHOP_ITEM) {
                    yml.set(base + ".price", slot.getPrice());
                    yml.set(base + ".amount", slot.getAmount());
                    yml.set(base + ".currency", slot.getCurrency().name());
                } else if (slot.getKind() == GuiSlot.Kind.SHOP_LINK) {
                    yml.set(base + ".linkedGui", slot.getLinkedGui());
                }
            }
            // s'assure que même un gui vide existe dans le fichier
            yml.createSection("shopGuis." + gui.getName());
        }

        for (var entry : config.getUpgradeGui().getSlots().entrySet()) {
            GuiSlot slot = entry.getValue();
            String base = "upgradeGui." + entry.getKey();
            yml.set(base + ".material", slot.getMaterial().name());
            yml.set(base + ".type", slot.getUpgradeType().name());
            yml.set(base + ".startLevel", slot.getStartLevel());
        }

        for (UpgradeTypeConfig cfg : config.getUpgradeTypeConfigs().values()) {
            String base = "upgradeTypeConfigs." + cfg.getType().name();
            yml.set(base + ".maxLevel", cfg.getMaxLevel());
            for (var priceEntry : cfg.getLevelPrices().entrySet()) {
                yml.set(base + ".prices." + priceEntry.getKey(), priceEntry.getValue());
            }
        }

        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder shop_config.yml: " + e.getMessage());
        }
    }
}
