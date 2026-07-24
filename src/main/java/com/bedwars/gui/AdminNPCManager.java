package com.bedwars.gui;

import com.bedwars.BedwarsPlugin;
import com.bedwars.util.LocationUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Gère un unique NPC (un Villageois sans IA) qui, lorsqu'on clique dessus,
 * ouvre l'interface d'administration listant toutes les arènes.
 * Remarque : pour un vrai NPC "cosmétique" (skin custom, pas de nom de mob),
 * un plugin dédié type Citizens serait idéal ; ici on utilise un Villageois
 * figé (IA désactivée, invulnérable) ce qui fonctionne nativement avec l'API Paper.
 */
public class AdminNPCManager {

    private final BedwarsPlugin plugin;
    private final File file;
    private UUID npcId;
    private Location npcLocation;

    public AdminNPCManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "npc.yml");
    }

    public void spawnAt(Location location) {
        removeNPC();
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setSilent(true);
        villager.setCollidable(false);
        villager.setCustomName(ChatColor.GOLD + "" + ChatColor.BOLD + "Admin Bedwars");
        villager.setCustomNameVisible(true);
        villager.setPersistent(true);
        this.npcId = villager.getUniqueId();
        this.npcLocation = location;
        save();
    }

    public boolean isNPC(UUID entityId) {
        return npcId != null && npcId.equals(entityId);
    }

    public void removeNPC() {
        if (npcId == null) return;
        org.bukkit.entity.Entity e = org.bukkit.Bukkit.getEntity(npcId);
        if (e != null) e.remove();
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        if (npcId != null) config.set("uuid", npcId.toString());
        LocationUtil.save(config, "location", npcLocation);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder le NPC admin: " + e.getMessage());
        }
    }

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String uuidStr = config.getString("uuid");
        Location loc = LocationUtil.load(config, "location");
        if (uuidStr == null || loc == null) return;
        this.npcId = UUID.fromString(uuidStr);
        this.npcLocation = loc;
        // Le NPC (entité Villageois) n'est pas persistant entre les redémarrages du monde
        // s'il n'a pas été sauvegardé par le monde lui-même ; on le respawn pour être sûr.
        if (org.bukkit.Bukkit.getEntity(npcId) == null) {
            spawnAt(loc);
        }
    }
}
