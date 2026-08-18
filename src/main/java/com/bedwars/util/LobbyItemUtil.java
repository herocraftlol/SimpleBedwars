package com.bedwars.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Les items donnés aux joueurs dans le lobby d'attente (avant le lancement de la partie) :
 *  - slot 1 (index 0) : diamant "Forcer le lancement", réservé aux admins.
 *  - slot 3 (index 2) : bloc "Choisir son équipe".
 *  - slot 5 (index 4) : bloc barrière "Quitter la partie".
 *
 * Chaque item est marqué par une donnée persistante pour être reconnu par les listeners
 * (ouverture de menu / action) et protégé (indéplaçable, indroppable) par KitProtectionListener.
 */
public final class LobbyItemUtil {

    public static final int SLOT_FORCE_START = 0;
    public static final int SLOT_TEAM_SELECT = 2;
    public static final int SLOT_LEAVE = 4;

    private static NamespacedKey forceStartKey;
    private static NamespacedKey teamSelectKey;
    private static NamespacedKey leaveKey;

    private LobbyItemUtil() {}

    public static void init(Plugin plugin) {
        forceStartKey = new NamespacedKey(plugin, "lobby_force_start");
        teamSelectKey = new NamespacedKey(plugin, "lobby_team_select");
        leaveKey = new NamespacedKey(plugin, "lobby_leave");
    }

    public static ItemStack createForceStartItem() {
        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Forcer le lancement");
        meta.setLore(java.util.List.of(
                ChatColor.GRAY + "Lance la partie immédiatement,",
                ChatColor.GRAY + "peu importe le nombre de joueurs.",
                "",
                ChatColor.DARK_GRAY + "Réservé aux admins."));
        meta.getPersistentDataContainer().set(forceStartKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createTeamSelectItem() {
        return createTeamSelectItem(null);
    }

    public static ItemStack createTeamSelectItem(com.bedwars.arena.TeamColor current) {
        Material material = current != null
                ? org.bukkit.Material.matchMaterial(current.getDyeColor().name() + "_WOOL")
                : Material.WHITE_WOOL;
        if (material == null) material = Material.WHITE_WOOL;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Choisir son équipe");
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add(ChatColor.GRAY + "Clique pour choisir l'équipe");
        lore.add(ChatColor.GRAY + "que tu veux rejoindre.");
        lore.add("");
        lore.add(current != null
                ? ChatColor.GREEN + "Équipe actuelle: " + current.getColoredName()
                : ChatColor.DARK_GRAY + "Aucune équipe choisie (aléatoire)");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(teamSelectKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createLeaveItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Quitter la partie");
        meta.getPersistentDataContainer().set(leaveKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isForceStart(ItemStack item) {
        return hasTag(item, forceStartKey);
    }

    public static boolean isTeamSelect(ItemStack item) {
        return hasTag(item, teamSelectKey);
    }

    public static boolean isLeave(ItemStack item) {
        return hasTag(item, leaveKey);
    }

    public static boolean isAnyLobbyItem(ItemStack item) {
        return isForceStart(item) || isTeamSelect(item) || isLeave(item);
    }

    private static boolean hasTag(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return false;
        Byte value = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }
}
