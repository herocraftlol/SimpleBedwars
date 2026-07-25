package com.bedwars.shop;

import com.bedwars.arena.GeneratorType;
import org.bukkit.Material;

/**
 * Un emplacement (0 à 53) d'un GUI de shop ou d'upgrade.
 * Selon le type de GUI, seuls certains champs sont pertinents :
 *  - Shop "item" simple : material + price + currency
 *  - Shop "lien vers un sous-gui" : material + linkedGui
 *  - Upgrade : material + upgradeType + startLevel
 */
public class GuiSlot {

    public enum Kind { EMPTY, SHOP_ITEM, SHOP_LINK, UPGRADE_ITEM }

    private Kind kind = Kind.EMPTY;
    private Material material;

    // Shop item
    private int price;
    private GeneratorType currency;

    // Shop link
    private String linkedGui;

    // Upgrade
    private UpgradeType upgradeType;
    private int startLevel = 1;

    public static GuiSlot empty() {
        return new GuiSlot();
    }

    public static GuiSlot shopItem(Material material, int price, GeneratorType currency) {
        GuiSlot slot = new GuiSlot();
        slot.kind = Kind.SHOP_ITEM;
        slot.material = material;
        slot.price = price;
        slot.currency = currency;
        return slot;
    }

    public static GuiSlot shopLink(Material material, String linkedGui) {
        GuiSlot slot = new GuiSlot();
        slot.kind = Kind.SHOP_LINK;
        slot.material = material;
        slot.linkedGui = linkedGui;
        return slot;
    }

    public static GuiSlot upgradeItem(Material material, UpgradeType type, int startLevel) {
        GuiSlot slot = new GuiSlot();
        slot.kind = Kind.UPGRADE_ITEM;
        slot.material = material;
        slot.upgradeType = type;
        slot.startLevel = startLevel;
        return slot;
    }

    public Kind getKind() {
        return kind;
    }

    public Material getMaterial() {
        return material;
    }

    public int getPrice() {
        return price;
    }

    public GeneratorType getCurrency() {
        return currency;
    }

    public String getLinkedGui() {
        return linkedGui;
    }

    public UpgradeType getUpgradeType() {
        return upgradeType;
    }

    public int getStartLevel() {
        return startLevel;
    }
}
