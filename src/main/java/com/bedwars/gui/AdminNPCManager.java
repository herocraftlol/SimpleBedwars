package com.bedwars.gui;

import com.bedwars.BedwarsPlugin;
import com.bedwars.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.UUID;

/**
 * Ancien gestionnaire du PNJ "hub" (Villageois cliquable ouvrant le GUI des arènes).
 * Cette fonctionnalité a été remplacée par la commande directe {@code /bd arene gui}
 * (voir BedwarsCommand#handleArene) : il n'y a donc plus de commande pour placer un
 * nouveau PNJ de ce type.
 *
 * Cette classe ne sert plus qu'à nettoyer, une fois pour toutes, les anciens PNJ hub
 * encore présents dans le monde (ils étaient persistants dans les versions précédentes,
 * donc toujours là après un redémarrage) : {@link #purgeLegacyNpc()} est appelée au
 * démarrage du plugin, supprime tout PNJ hub résiduel trouvé à son ancien emplacement
 * sauvegardé, puis efface définitivement ce fichier de sauvegarde pour que le nettoyage
 * n'ait besoin de tourner qu'une seule fois.
 */
public class AdminNPCManager {

    private static NamespacedKey markerKey;

    private final BedwarsPlugin plugin;
    private final File file;
    private UUID npcId;

    public AdminNPCManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "npc.yml");
        if (markerKey == null) {
            markerKey = new NamespacedKey(plugin, "bedwars_hub_npc");
        }
    }

    /** Ne reconnaît plus aucun PNJ comme "hub" : la fonctionnalité a été retirée. */
    public boolean isNPC(UUID entityId) {
        return false;
    }

    public void removeNPC() {
        // Conservé pour compatibilité (appelé depuis onDisable) ; ne fait plus rien de spécifique
        // puisque purgeLegacyNpc() a déjà tout nettoyé au démarrage.
    }

    /**
     * Supprime définitivement tout ancien PNJ hub encore présent dans le monde (en chargeant
     * son chunk pour être sûr de le trouver, même après un redémarrage), puis efface son fichier
     * de sauvegarde. Idempotent : ne fait plus rien dès que le nettoyage a déjà eu lieu une fois.
     */
    public void purgeLegacyNpc() {
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Location loc = LocationUtil.load(config, "location");
        if (loc != null && loc.getWorld() != null) {
            loc.getChunk().load();
            for (Entity entity : loc.getWorld().getNearbyEntities(loc, 5, 5, 5)) {
                if (isLegacyHubNpc(entity)) {
                    entity.remove();
                }
            }
        }

        if (!file.delete()) {
            plugin.getLogger().warning("Impossible de supprimer l'ancien fichier npc.yml (PNJ hub obsolète).");
        }
    }

    private boolean isLegacyHubNpc(Entity entity) {
        // Marqué par notre tag (versions récentes) OU, à défaut, un Villageois figé nommé
        // "Admin Bedwars" (versions plus anciennes, jamais tagué) : on couvre les deux cas.
        Byte tagged = entity.getPersistentDataContainer().get(markerKey, PersistentDataType.BYTE);
        if (tagged != null && tagged == (byte) 1) return true;
        return entity.getType() == org.bukkit.entity.EntityType.VILLAGER
                && entity.getCustomName() != null
                && org.bukkit.ChatColor.stripColor(entity.getCustomName()).equals("Admin Bedwars");
    }
}
