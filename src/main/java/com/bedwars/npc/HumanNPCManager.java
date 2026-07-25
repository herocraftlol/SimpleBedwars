package com.bedwars.npc;

import com.bedwars.BedwarsPlugin;
import com.bedwars.npc.provider.NpcProvider;
import com.bedwars.npc.provider.ProtocolLibNpcProvider;
import com.bedwars.npc.provider.VillagerNpcProvider;
import com.bedwars.shop.GuiDefinition;
import com.bedwars.shop.ShopType;
import com.bedwars.util.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère les NPC "Shop" et "Upgrade" (stationnaires, regardent le joueur le plus proche).
 *
 * Cette classe NE référence jamais directement les types ProtocolLib : le vrai travail
 * bas-niveau est délégué à {@link ProtocolLibNpcProvider}, qui n'est instanciée (et donc
 * chargée par la JVM) QUE si le plugin ProtocolLib est détecté au démarrage. Si ce n'est
 * pas le cas, on utilise {@link VillagerNpcProvider} (villageois classique, sans skin de
 * joueur mais garanti fonctionnel sans dépendance externe).
 */
public class HumanNPCManager implements Listener {

    private final BedwarsPlugin plugin;
    private final File file;
    private final Map<UUID, NpcRecord> npcs = new ConcurrentHashMap<>();
    private final NpcProvider provider;
    private final boolean protocolLibMode;

    public HumanNPCManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "shop_npcs.yml");
        Bukkit.getPluginManager().registerEvents(this, plugin);

        boolean hasProtocolLib = Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;
        NpcProvider chosenProvider;
        if (hasProtocolLib) {
            try {
                chosenProvider = new ProtocolLibNpcProvider(plugin, this::onNpcClicked);
                plugin.getLogger().info("ProtocolLib détecté : NPC shop/upgrade avec skin de joueur activés.");
            } catch (Throwable t) {
                plugin.getLogger().warning("ProtocolLib présent mais l'initialisation a échoué ("
                        + t.getMessage() + "), repli sur des villageois classiques.");
                chosenProvider = new VillagerNpcProvider();
                hasProtocolLib = false;
            }
        } else {
            plugin.getLogger().warning("ProtocolLib n'est pas installé : les NPC shop/upgrade seront des "
                    + "villageois classiques (sans skin de joueur). Installe ProtocolLib pour activer le skin.");
            chosenProvider = new VillagerNpcProvider();
        }
        this.provider = chosenProvider;
        this.protocolLibMode = hasProtocolLib;

        if (!protocolLibMode) {
            Bukkit.getPluginManager().registerEvents(new VillagerInteractBridge(this), plugin);
        }

        Bukkit.getScheduler().runTaskTimer(plugin, this::rotateAll, 10L, 4L);
    }

    public NpcRecord spawn(ShopType type, Location location) {
        NpcRecord record = new NpcRecord(UUID.randomUUID(), type, location.clone());
        npcs.put(record.getId(), record);
        provider.spawn(record);
        save();
        return record;
    }

    public void removeAll() {
        for (NpcRecord record : npcs.values()) {
            provider.despawn(record);
        }
        npcs.clear();
    }

    public NpcRecord getByEntityUuid(UUID entityUuid) {
        for (NpcRecord record : npcs.values()) {
            if (record.getEntityUuid() != null && record.getEntityUuid().equals(entityUuid)) return record;
        }
        return null;
    }

    private void onNpcClicked(Player player, UUID npcId) {
        NpcRecord record = npcs.get(npcId);
        if (record == null) return;
        plugin.getShopGUIManager().open(player, record.getType(), GuiDefinition.BASE, false);
    }

    private void rotateAll() {
        for (NpcRecord record : npcs.values()) {
            Player nearest = findNearest(record.getLocation(), 8.0);
            if (nearest != null) provider.lookAt(record, nearest);
        }
    }

    private Player findNearest(Location loc, double maxDistance) {
        if (loc.getWorld() == null) return null;
        Player nearest = null;
        double best = maxDistance * maxDistance;
        for (Player p : loc.getWorld().getPlayers()) {
            double dist = p.getLocation().distanceSquared(loc);
            if (dist < best) {
                best = dist;
                nearest = p;
            }
        }
        return nearest;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        for (NpcRecord record : npcs.values()) {
            provider.showTo(record, event.getPlayer());
        }
    }

    // -----------------------------------------------------------------
    // Persistance
    // -----------------------------------------------------------------

    private void save() {
        YamlConfiguration yml = new YamlConfiguration();
        int i = 0;
        for (NpcRecord record : npcs.values()) {
            String base = "npcs." + i;
            yml.set(base + ".type", record.getType().name());
            LocationUtil.save(yml, base + ".location", record.getLocation());
            i++;
        }
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder shop_npcs.yml: " + e.getMessage());
        }
    }

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        if (!yml.isConfigurationSection("npcs")) return;
        for (String key : Objects.requireNonNull(yml.getConfigurationSection("npcs")).getKeys(false)) {
            String base = "npcs." + key;
            String typeName = yml.getString(base + ".type");
            Location loc = LocationUtil.load(yml, base + ".location");
            if (typeName == null || loc == null) continue;
            spawn(ShopType.valueOf(typeName), loc);
        }
    }

    /** Utilisé uniquement par {@link VillagerInteractBridge} en mode de repli (sans ProtocolLib). */
    void handleClickFromBridge(Player player, NpcRecord record) {
        onNpcClicked(player, record.getId());
    }
}
