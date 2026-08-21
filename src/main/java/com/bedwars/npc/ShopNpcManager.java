package com.bedwars.npc;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.ArenaTeam;
import com.bedwars.arena.TeamColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import com.destroystokyo.paper.profile.PlayerProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gère les PNJ "Marchand" et "Amélioration" : des mobs immobiles (IA désactivée, invulnérables)
 * habillés d'une tête de joueur et d'une armure en cuir teintée à la couleur de l'équipe, un par
 * équipe et par arène. Cliquer dessus ouvre le GUI shop ou améliorations correspondant.
 *
 * Pourquoi un Zombie plutôt qu'un Armor Stand : les Armor Stands utilisent un événement de clic
 * séparé et "positionnel" (PlayerInteractAtEntityEvent, avec la position exacte du clic sur le
 * corps), ce qui les rend peu fiables pour ouvrir un menu de façon garantie selon l'endroit
 * précis cliqué. Un mob classique (ici un Zombie, IA/dégâts/combustion désactivés) déclenche
 * toujours l'événement standard et fiable PlayerInteractEntityEvent, quel que soit l'endroit
 * cliqué sur son corps — d'où ce choix, malgré l'absence de vrai skin de joueur sans plugin
 * tiers (Citizens) ou paquets réseau bruts. La tête de joueur (skin réel si un pseudo est
 * fourni) + l'armure en cuir teintée suffisent à donner une allure de "PNJ vendeur" correcte.
 *
 * Anti-duplication : ces PNJ ne sont pas persistés par le monde (setPersistent(false)) — c'est
 * le plugin qui les fait toujours réapparaître lui-même au démarrage (spawnAll()). Avant chaque
 * apparition, le chunk concerné est chargé puis purgé de tout PNJ résiduel marqué par notre tag
 * interne, ce qui nettoie aussi les doublons éventuellement déjà accumulés par le passé.
 */
public class ShopNpcManager {

    public enum NpcType { SHOP, UPGRADE }

    public record NpcInfo(String arenaName, TeamColor team, NpcType type) {}

    private static NamespacedKey markerKey;

    private final BedwarsPlugin plugin;
    private final Map<UUID, NpcInfo> registry = new HashMap<>();

    public ShopNpcManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    public static void init(BedwarsPlugin plugin) {
        markerKey = new NamespacedKey(plugin, "bedwars_shop_npc");
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
        purgeStrayNpcsNear(location);

        Zombie npc = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
        npc.setBaby(false);
        npc.setAI(false);
        npc.setInvulnerable(true);
        npc.setSilent(true);
        npc.setCollidable(false);
        npc.setCanPickupItems(false);
        npc.setShouldBurnInDay(false); // sinon il prend feu en plein jour malgré l'invulnérabilité
        npc.setRemoveWhenFarAway(false);
        npc.setPersistent(false); // le plugin le respawn lui-même : jamais sauvegardé par le monde
        npc.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);

        String label = type == NpcType.SHOP ? "Marchand" : "Amélioration";
        npc.setCustomName(color.getChatColor() + "" + ChatColor.BOLD + label + ChatColor.RESET
                + " " + color.getColoredName());
        npc.setCustomNameVisible(true);

        // Tenue : tête de joueur + plastron/jambières/bottes en cuir teintés à la couleur de l'équipe.
        npc.getEquipment().setHelmet(buildHead());
        npc.getEquipment().setChestplate(dyed(Material.LEATHER_CHESTPLATE, color));
        npc.getEquipment().setLeggings(dyed(Material.LEATHER_LEGGINGS, color));
        npc.getEquipment().setBoots(dyed(Material.LEATHER_BOOTS, color));
        npc.getEquipment().setHelmetDropChance(0f);
        npc.getEquipment().setChestplateDropChance(0f);
        npc.getEquipment().setLeggingsDropChance(0f);
        npc.getEquipment().setBootsDropChance(0f);
        npc.getEquipment().setItemInMainHandDropChance(0f);

        String effectiveSkin = skinName != null ? skinName
                : plugin.getConfig().getString("npc.default-skin", null);
        if (effectiveSkin != null && !effectiveSkin.isBlank()) {
            applySkinAsync(npc, effectiveSkin);
        }

        registry.put(npc.getUniqueId(), new NpcInfo(arena.getName(), color, type));
    }

    private ItemStack buildHead() {
        return new ItemStack(Material.PLAYER_HEAD);
    }

    /**
     * Charge le chunk de cet emplacement et supprime tout PNJ marchand/amélioration résiduel
     * trouvé EXACTEMENT au même endroit (doublons d'anciennes sessions, ou re-configuration au
     * même endroit). Rayon volontairement très petit : un rayon plus large supprimerait aussi un
     * PNJ différent placé juste à côté (ex: le marchand et l'amélioration d'une même équipe, ou
     * ceux d'une équipe voisine), ce qui les faisait disparaître les uns les autres.
     */
    private void purgeStrayNpcsNear(Location location) {
        location.getChunk().load();
        for (Entity entity : location.getWorld().getNearbyEntities(location, 0.5, 0.5, 0.5)) {
            if (isOurNpc(entity)) {
                registry.remove(entity.getUniqueId());
                entity.remove();
            }
        }
    }

    private boolean isOurNpc(Entity entity) {
        if (markerKey == null) return false;
        Byte value = entity.getPersistentDataContainer().get(markerKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    private void applySkinAsync(Zombie npc, String skinName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PlayerProfile profile = Bukkit.createProfile(skinName);
                profile.complete(true); // appel réseau bloquant -> hors thread principal
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!npc.isValid()) return;
                    ItemStack head = npc.getEquipment().getHelmet();
                    if (head == null || head.getType() != Material.PLAYER_HEAD) return;
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    meta.setOwnerProfile(profile);
                    head.setItemMeta(meta);
                    npc.getEquipment().setHelmet(head);
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

    /** Supprime tous les PNJ (shop + amélioration) d'une arène donnée, y compris les doublons résiduels. */
    public void removeForArena(Arena arena) {
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, NpcInfo> entry : registry.entrySet()) {
            if (entry.getValue().arenaName().equalsIgnoreCase(arena.getName())) {
                toRemove.add(entry.getKey());
            }
        }
        for (UUID id : toRemove) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) entity.remove();
            registry.remove(id);
        }
        // Filet de sécurité : purge aussi tout PNJ résiduel non suivi par le registre (doublons
        // d'anciennes sessions), en se basant sur les emplacements connus de cette arène.
        for (ArenaTeam team : arena.getTeams().values()) {
            if (team.getShopLocation() != null) purgeStrayNpcsNear(team.getShopLocation());
            if (team.getUpgradeLocation() != null) purgeStrayNpcsNear(team.getUpgradeLocation());
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
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) entity.remove();
        }
        registry.clear();
    }
}
