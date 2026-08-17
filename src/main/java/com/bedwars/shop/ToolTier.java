package com.bedwars.shop;

import org.bukkit.Material;

/**
 * Paliers de pioche/hache de l'onglet spécial "Tools" du shop : bois -> fer -> or -> diamant.
 * Comportement façon Hypixel Bedwars : à chaque mort, l'outil redescend d'un palier
 * (voir GameInstance#giveKit / #downgradeTools), jusqu'au palier bois qui, lui, est
 * définitivement acquis et ne redescend jamais plus bas (il fait partie du kit de base).
 */
public enum ToolTier {

    WOOD(0, Material.WOODEN_PICKAXE, Material.WOODEN_AXE, null, 0),
    IRON(1, Material.IRON_PICKAXE, Material.IRON_AXE, Material.IRON_INGOT, 10),
    GOLD(2, Material.GOLDEN_PICKAXE, Material.GOLDEN_AXE, Material.GOLD_INGOT, 6),
    DIAMOND(3, Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.EMERALD, 4);

    private final int level;
    private final Material pickaxe;
    private final Material axe;
    private final Material currency;
    private final int price;

    ToolTier(int level, Material pickaxe, Material axe, Material currency, int price) {
        this.level = level;
        this.pickaxe = pickaxe;
        this.axe = axe;
        this.currency = currency;
        this.price = price;
    }

    public int getLevel() {
        return level;
    }

    public Material getPickaxe() {
        return pickaxe;
    }

    public Material getAxe() {
        return axe;
    }

    /** null pour le palier bois (gratuit, toujours acquis). */
    public Material getCurrency() {
        return currency;
    }

    public int getPrice() {
        return price;
    }

    public static ToolTier byLevel(int level) {
        for (ToolTier tier : values()) {
            if (tier.level == level) return tier;
        }
        return WOOD;
    }

    public ToolTier next() {
        return byLevel(Math.min(level + 1, DIAMOND.level));
    }

    public ToolTier previous() {
        return byLevel(Math.max(level - 1, WOOD.level));
    }
}
