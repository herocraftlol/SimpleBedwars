package com.bedwars.npc;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.ArenaTeam;
import com.bedwars.arena.TeamColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gère les PNJ "Marchand" et "Amélioration" : des Armor Stands immobiles à taille et pose
 * de joueur (tête de joueur + armure en cuir teintée à la couleur de l'équipe), un par équipe
 * et par arène. Cliquer dessus ouvre le GUI shop ou améliorations correspondant.
 *
 * Remarque : sans plugin tiers (Citizens) ou paquets réseau bruts, il n'existe pas de moyen
 * public de faire apparaître une vraie entité "Joueur" via l'API Bukkit/Paper. On utilise donc
 * un Armor Stand habillé d'une tête de joueur (skin réel si un pseudo est fourni, récupéré de
 * façon asynchrone via l'API Mojang) : silhouette et pose humaines, immobile, cliquable.
 */
public class ShopNpcManager {

    public enum NpcType { SHOP, UPGRADE }

    public record NpcInfo(String arenaName, TeamColor team, NpcType type) {}

    private final BedwarsPlugin plugin;
    private final Map<UUID, NpcInfo> registry = new HashMap<>();

    public ShopNpcManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    public NpcInfo getInfo(UUID entityId) {
        return registry.get(entityId);
    }

    /** (Re)fait apparaître tous les PNJ configurés pour une arène (appelé au chargement / après /bd ... save). */
    public void spawnForArena(Arena arena) {
        removeForArena(arena);
        for (Map.Entry<TeamColor, ArenaTeam> entry : arena.getTeams().entrySet()) {
            ArenaTeam team = entry.getValue();
            if (team.getShopLocation() != null) {
                spawnOne(arena, entry.getKey(), NpcType.SHOP, team.getShopLocation(), null);
            }
            if (team.getUpgradeLocation() != null) {
                spawnOne(arena, entry.getKey(), NpcType.UPGRADE, team.getUpgradeLocation(), null);
            }
        }
    }

    /** Fait apparaître (ou remplace) un PNJ précis. skinName est optionnel (pseudo dont copier le skin). */
    public void spawnOne(Arena arena, TeamColor color, NpcType type, Location location, String skinName) {
        ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        stand.setInvulnerable(true);
        stand.setGravity(false);
        stand.setPersistent(true);
        stand.setArms(true);
        stand.setBasePlate(false);
        stand.setSmall(false);
        stand.setCollidable(false);

        String label = type == NpcType.SHOP ? "Marchand" : "Amélioration";
        stand.setCustomName(color.getChatColor() + "" + ChatColor.BOLD + label + ChatColor.RESET
                + " " + color.getColoredName());
        stand.setCustomNameVisible(true);

        // Tenue : plastron/jambières/bottes en cuir teintés à la couleur de l'équipe.
        stand.getEquipment().setChestplate(dyed(Material.LEATHER_CHESTPLATE, color));
        stand.getEquipment().setLeggings(dyed(Material.LEATHER_LEGGINGS, color));
        stand.getEquipment().setBoots(dyed(Material.LEATHER_BOOTS, color));

        // Tête : tête de joueur (skin par défaut du serveur, ou skin réel si un pseudo est précisé).
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        stand.getEquipment().setHelmet(head);

        String effectiveSkin = skinName != null ? skinName
                : plugin.getConfig().getString("npc.default-skin", null);
        if (effectiveSkin != null && !effectiveSkin.isBlank()) {
            applySkinAsync(stand, effectiveSkin);
        }

        registry.put(stand.getUniqueId(), new NpcInfo(arena.getName(), color, type));
    }

    private void applySkinAsync(ArmorStand stand, String skinName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PlayerProfile profile = Bukkit.createProfile(skinName);
                // update() interroge les serveurs Mojang de façon asynchrone -> hors thread principal
                profile = profile.update().join();
                final PlayerProfile filled = profile;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!stand.isValid()) return;
                    ItemStack head = stand.getEquipment().getHelmet();
                    if (head == null || head.getType() != Material.PLAYER_HEAD) return;
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    meta.setOwnerProfile(filled);
                    head.setItemMeta(meta);
                    stand.getEquipment().setHelmet(head);
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Impossible de récupérer le skin '" + skinName + "': " + e.getMessage());
            }
        });
    }

    private ItemStack dyed(Material material, TeamColor color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color.getArmorColor());
        item.setItemMeta(meta);
        return item;
    }

    /** Supprime tous les PNJ (shop + amélioration) d'une arène donnée. */
    public void removeForArena(Arena arena) {
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, NpcInfo> entry : registry.entrySet()) {
            if (entry.getValue().arenaName().equalsIgnoreCase(arena.getName())) {
                toRemove.add(entry.getKey());
            }
        }
        for (UUID id : toRemove) {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(id);
            if (entity != null) entity.remove();
            registry.remove(id);
        }
    }

    /** Fait réapparaître les PNJ de toutes les arènes sauvegardées (appelé au démarrage du plugin). */
    public void spawnAll() {
        for (Arena arena : plugin.getArenaManager().getArenas().values()) {
            if (arena.isSaved()) {
                spawnForArena(arena);
            }
        }
    }

    public void removeAll() {
        for (UUID id : new ArrayList<>(registry.keySet())) {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(id);
            if (entity != null) entity.remove();
        }
        registry.clear();
    }
}
