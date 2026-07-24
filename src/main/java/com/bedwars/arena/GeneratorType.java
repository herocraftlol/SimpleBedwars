package com.bedwars.arena;

import org.bukkit.Material;

public enum GeneratorType {
    FER(Material.IRON_INGOT, false),
    OR(Material.GOLD_INGOT, false),
    DIAMOND(Material.DIAMOND, true),
    EMERAUDE(Material.EMERALD, true);

    private final Material material;
    private final boolean capped; // limité par un nombre max au sol

    GeneratorType(Material material, boolean capped) {
        this.material = material;
        this.capped = capped;
    }

    public Material getMaterial() {
        return material;
    }

    public boolean isCapped() {
        return capped;
    }

    public static GeneratorType fromInput(String input) {
        if (input == null) return null;
        String s = input.toLowerCase();
        return switch (s) {
            case "fer", "iron" -> FER;
            case "or", "gold" -> OR;
            case "diamand", "diamant", "diamond" -> DIAMOND;
            case "emeraude", "émeraude", "emerald" -> EMERAUDE;
            default -> null;
        };
    }
}
