package com.bedwars.shop;

import org.bukkit.Material;

/**
 * Paliers d'épée (comme {@link ToolTier} pour la pioche/hache) : bois -> pierre -> fer -> diamant.
 * L'épée en bois de base (toujours au slot 1 de la hotbar) peut être améliorée dans le shop ;
 * à chaque mort elle redescend d'un palier (jamais en dessous du bois, qui fait partie du kit).
 */
public enum SwordTier {

    WOOD(0, Material.WOODEN_SWORD, null, 0),
    STONE(1, Material.STONE_SWORD, Material.IRON_INGOT, 10),
    IRON(2, Material.IRON_SWORD, Material.GOLD_INGOT, 7),
    DIAMOND(3, Material.DIAMOND_SWORD, Material.EMERALD, 4);

    private final int level;
    private final Material material;
    private final Material currency;
    private final int price;

    SwordTier(int level, Material material, Material currency, int price) {
        this.level = level;
        this.material = material;
        this.currency = currency;
        this.price = price;
    }

    public int getLevel() {
        return level;
    }

    public Material getMaterial() {
        return material;
    }

    /** null pour le palier bois (gratuit, toujours acquis). */
    public Material getCurrency() {
        return currency;
    }

    public int getPrice() {
        return price;
    }

    public static SwordTier byLevel(int level) {
        for (SwordTier tier : values()) {
            if (tier.level == level) return tier;
        }
        return WOOD;
    }

    public SwordTier next() {
        return byLevel(Math.min(level + 1, DIAMOND.level));
    }
}
