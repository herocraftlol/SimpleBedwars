package com.bedwars.shop;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionType;

import java.util.Map;

/**
 * Un article du shop. La forme la plus courante (utilisée par /bd shop <catégorie> <slot> <item>
 * <quantité> <prix> <minerai>) est volontairement simple : "achète N x item pour un prix".
 *
 * Deux formes spéciales existent en plus (utilisées uniquement par les articles pré-configurés
 * de l'onglet Potions/Ranged, voir ShopConfigManager#seedDefaults) :
 *  - {@link #potion} : une potion (jetable) d'un type/durée précis, avec la bonne couleur de base.
 *  - {@link #enchantedItem} : un item avec des enchantements fixes (arcs Puissance/Recul...).
 *
 * L'onglet spécial "Tools" (pioche/hache à paliers) reste géré entièrement à part (voir ToolTier).
 */
public class ShopItem {

    private final Material material;
    private final int amount;
    private final Material currency;
    private final int price;
    private final String displayNameOverride;
    private final PotionType potionType;
    private final int potionDurationTicks;
    private final Map<Enchantment, Integer> enchantments;

    public ShopItem(Material material, int amount, Material currency, int price) {
        this(material, amount, currency, price, null, null, 0, Map.of());
    }

    private ShopItem(Material material, int amount, Material currency, int price, String displayNameOverride,
                      PotionType potionType, int potionDurationTicks, Map<Enchantment, Integer> enchantments) {
        this.material = material;
        this.amount = amount;
        this.currency = currency;
        this.price = price;
        this.displayNameOverride = displayNameOverride;
        this.potionType = potionType;
        this.potionDurationTicks = potionDurationTicks;
        this.enchantments = enchantments;
    }

    /** Potion jetable (SPLASH_POTION) d'un type et d'une durée précis, avec la couleur vanilla correspondante. */
    public static ShopItem potion(String displayName, PotionType type, int durationSeconds, Material currency, int price) {
        return new ShopItem(Material.SPLASH_POTION, 1, currency, price, displayName, type, durationSeconds * 20, Map.of());
    }

    /** Item avec des enchantements fixes (ex: arc Puissance I). */
    public static ShopItem enchantedItem(Material material, String displayName, Material currency, int price,
                                          Map<Enchantment, Integer> enchantments) {
        return new ShopItem(material, 1, currency, price, displayName, null, 0, enchantments);
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

    public PotionType getPotionType() {
        return potionType;
    }

    public int getPotionDurationTicks() {
        return potionDurationTicks;
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }

    /** Nom d'affichage : celui fourni explicitement, sinon généré à partir du nom du matériau (WHITE_WOOL -> "White Wool"). */
    public String getDisplayName() {
        if (displayNameOverride != null) return displayNameOverride;
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
