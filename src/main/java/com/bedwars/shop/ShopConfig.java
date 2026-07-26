package com.bedwars.shop;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration GLOBALE (partagée entre toutes les arènes) des GUI shop et upgrade.
 * Le shop peut avoir plusieurs GUI nommés (base + sous-gui créés via addgui).
 * L'upgrade n'a qu'un seul GUI ("base").
 */
public class ShopConfig {

    private final Map<String, GuiDefinition> shopGuis = new LinkedHashMap<>();
    private final GuiDefinition upgradeGui = new GuiDefinition(GuiDefinition.BASE);
    private final Map<UpgradeType, UpgradeTypeConfig> upgradeTypeConfigs = new LinkedHashMap<>();

    public ShopConfig() {
        shopGuis.put(GuiDefinition.BASE, new GuiDefinition(GuiDefinition.BASE));
        for (UpgradeType type : UpgradeType.values()) {
            upgradeTypeConfigs.put(type, new UpgradeTypeConfig(type));
        }
    }

    public GuiDefinition getOrCreateShopGui(String name) {
        return shopGuis.computeIfAbsent(name.toLowerCase(), GuiDefinition::new);
    }

    public GuiDefinition getShopGui(String name) {
        return shopGuis.get(name.toLowerCase());
    }

    public boolean shopGuiExists(String name) {
        return shopGuis.containsKey(name.toLowerCase());
    }

    public void deleteShopGui(String name) {
        if (name.equalsIgnoreCase(GuiDefinition.BASE)) return; // on ne supprime jamais la base
        shopGuis.remove(name.toLowerCase());
    }

    public Map<String, GuiDefinition> getShopGuis() {
        return shopGuis;
    }

    public GuiDefinition getUpgradeGui() {
        return upgradeGui;
    }

    public UpgradeTypeConfig getUpgradeConfig(UpgradeType type) {
        return upgradeTypeConfigs.computeIfAbsent(type, UpgradeTypeConfig::new);
    }

    public Map<UpgradeType, UpgradeTypeConfig> getUpgradeTypeConfigs() {
        return upgradeTypeConfigs;
    }
}
