package com.bedwars.util;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Les 3 outils du kit de base, verrouillés dans la hotbar de chaque joueur en partie :
 *  - slot 1 (index 0) : épée (palier bois par défaut, améliorable dans le shop) ;
 *  - slot 2 (index 1) : hache (palier bois par défaut, améliorable dans le shop) ;
 *  - slot 3 (index 2) : pioche (palier bois par défaut, améliorable dans le shop).
 * Marqués par une donnée persistante afin que {@code KitProtectionListener} puisse les
 * reconnaître (quel que soit leur palier/matériau actuel) et empêcher de les drop, déplacer,
 * dupliquer ou enlever de quelque façon que ce soit.
 */
public final class KitProtectionUtil {

    public static final int SLOT_SWORD = 0;
    public static final int SLOT_AXE = 1;
    public static final int SLOT_PICKAXE = 2;

    private static NamespacedKey key;

    private KitProtectionUtil() {}

    public static void init(Plugin plugin) {
        key = new NamespacedKey(plugin, "protected_kit_tool");
    }

    /** Marque un item comme outil de kit protégé (à appeler avant de le placer dans l'inventaire). */
    public static ItemStack tagAsKitTool(ItemStack item, String displayName) {
        ItemMeta meta = item.getItemMeta();
        if (displayName != null) meta.setDisplayName(ChatColor.RESET + displayName);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isKitTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        Byte value = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    /** Conservé pour compatibilité : l'ancien nom ne concernait que l'épée, désormais les 3 outils. */
    public static boolean isProtectedSword(ItemStack item) {
        return isKitTool(item);
    }
}
