package com.bedwars.shop;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionType;

import java.util.List;
import java.util.Map;

/**
 * Articles spéciaux ajoutés automatiquement aux onglets "potions" et "ranged" du shop, en plus
 * de ce que l'admin configure via /bd shop. Volontairement gardés à part (comme l'onglet Tools)
 * et jamais écrits dans shop.yml : le système de configuration générique ne sait sérialiser que
 * matériau/quantité/prix/monnaie, ce qui ferait perdre le type de potion ou les enchantements au
 * premier redémarrage si on les y stockait.
 */
public final class SpecialShopItems {

    private SpecialShopItems() {}

    public static final List<ShopItem> POTIONS = List.of(
            ShopItem.potion("Potion de Force (20s)", PotionType.STRENGTH, 20, Material.EMERALD, 4),
            ShopItem.potion("Potion de Vitesse (20s)", PotionType.SWIFTNESS, 20, Material.EMERALD, 2),
            ShopItem.potion("Potion d'Invisibilité (20s)", PotionType.INVISIBILITY, 20, Material.EMERALD, 4),
            ShopItem.potion("Potion de Saut (20s)", PotionType.LEAPING, 20, Material.EMERALD, 2),
            ShopItem.potion("Potion de Soin", PotionType.HEALING, 0, Material.EMERALD, 2)
    );

    public static final List<ShopItem> RANGED_EXTRA = List.of(
            ShopItem.enchantedItem(Material.BOW, "Arc (Puissance I)", Material.IRON_INGOT, 24,
                    Map.of(Enchantment.POWER, 1)),
            ShopItem.enchantedItem(Material.BOW, "Arc (Puissance I, Recul I)", Material.EMERALD, 6,
                    Map.of(Enchantment.POWER, 1, Enchantment.PUNCH, 1))
    );
}
