package com.bedwars.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * L'épée en bois de base, toujours présente au slot 0 (premier slot de la hotbar) de
 * chaque joueur en partie. Marquée par une donnée persistante afin que
 * {@code KitProtectionListener} puisse la reconnaître et empêcher de la drop, déplacer,
 * dupliquer ou enlever de quelque façon que ce soit.
 */
public final class KitProtectionUtil {

    private static NamespacedKey key;

    private KitProtectionUtil() {}

    public static void init(Plugin plugin) {
        key = new NamespacedKey(plugin, "protected_kit_sword");
    }

    public static ItemStack createProtectedSword() {
        ItemStack sword = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + "Épée en bois");
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        sword.setItemMeta(meta);
        return sword;
    }

    public static boolean isProtectedSword(ItemStack item) {
        if (item == null || item.getType() != Material.WOODEN_SWORD || !item.hasItemMeta()) return false;
        Byte value = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }
}
