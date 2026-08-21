package com.bedwars.shop;

import org.bukkit.Material;

/**
 * Paliers de pioche/hache de l'onglet spécial "Tools" du shop : bois -> pierre -> fer -> diamant
 * (mêmes noms de palier que l'épée, voir SwordTier). Contrairement à l'épée, aucun palier n'est
 * gratuit : il faut acheter le palier bois pour avoir sa toute première pioche/hache.
 * Comportement façon Hypixel Bedwars : à chaque mort, l'outil redescend d'un palier une fois
 * acheté (voir GameInstance#downgradeTools), jusqu'au palier bois qui, lui, reste définitivement
 * acquis une fois obtenu (ne redescend jamais plus bas, ni ne disparaît).
 */
public enum ToolTier {

    WOOD(0, Material.WOODEN_PICKAXE, Material.WOODEN_AXE, Material.IRON_INGOT, 10),
    STONE(1, Material.STONE_PICKAXE, Material.STONE_AXE, Material.IRON_INGOT, 10),
    IRON(2, Material.IRON_PICKAXE, Material.IRON_AXE, Material.GOLD_INGOT, 4),
    DIAMOND(3, Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.GOLD_INGOT, 12);

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

    /** Palier suivant celui donné ; à appeler uniquement pour level >= 0 (voir ToolTier.WOOD pour level == -1, "pas encore achetée"). */
    public ToolTier next() {
        return byLevel(Math.min(level + 1, DIAMOND.level));
    }

    public ToolTier previous() {
        return byLevel(Math.max(level - 1, WOOD.level));
    }
}
