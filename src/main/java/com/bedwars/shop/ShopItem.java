package com.bedwars.shop;

import org.bukkit.Material;

/**
 * Un article générique du shop, entièrement défini en jeu via :
 * /bd shop <catégorie> <slot> <item> <quantité> <prix> <minerai>
 *
 * Volontairement simple (juste "achète N x item pour un prix") : la seule exception
 * est l'onglet spécial "Tools" (pioche/hache à paliers, voir {@link ToolTier}), qui
 * reste géré à part car son comportement (dégradation à la mort) est spécifique.
 */
public class ShopItem {

    private final Material material;
    private final int amount;
    private final Material currency;
    private final int price;

    public ShopItem(Material material, int amount, Material currency, int price) {
        this.material = material;
        this.amount = amount;
        this.currency = currency;
        this.price = price;
    }

    public Material getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    public Material getCurrency() {
        return currency;
    }

    public int getPrice() {
        return price;
    }

    /** Nom d'affichage généré automatiquement à partir du nom du matériau (ex: WHITE_WOOL -> "White Wool"). */
    public String getDisplayName() {
        String[] parts = material.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    public String getCurrencyName() {
        return switch (currency) {
            case IRON_INGOT -> "Fer";
            case GOLD_INGOT -> "Or";
            case EMERALD -> "Émeraude" + (price > 1 ? "s" : "");
            case DIAMOND -> "Diamant" + (price > 1 ? "s" : "");
            default -> currency.name();
        };
    }
}
