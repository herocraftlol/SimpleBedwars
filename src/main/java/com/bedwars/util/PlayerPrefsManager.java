package com.bedwars.util;

import com.bedwars.BedwarsPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Préférences personnelles de chaque joueur pour la position dans la hotbar de ses items de
 * kit (épée/hache/pioche) et de l'item "choisir son équipe" (bloc de laine) du lobby — voir
 * /bd quickmenu. Persisté dans playerprefs.yml, accessible et modifiable par n'importe quel joueur.
 */
public class PlayerPrefsManager {

    public static final String SWORD = "sword";
    public static final String AXE = "axe";
    public static final String PICKAXE = "pickaxe";
    public static final String BLOCK = "block";

    private final BedwarsPlugin plugin;
    private final File file;
    private final Map<UUID, Map<String, Integer>> prefs = new HashMap<>();

    public PlayerPrefsManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "playerprefs.yml");
    }

    public void load() {
        prefs.clear();
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String uuidKey : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidKey);
                Map<String, Integer> playerPrefs = new HashMap<>();
                for (String key : new String[]{SWORD, AXE, PICKAXE, BLOCK}) {
                    if (config.contains(uuidKey + "." + key)) {
                        playerPrefs.put(key, config.getInt(uuidKey + "." + key));
                    }
                }
                prefs.put(uuid, playerPrefs);
            } catch (IllegalArgumentException ignored) {
                // clé invalide dans le fichier : on ignore simplement
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, Integer>> entry : prefs.entrySet()) {
            for (Map.Entry<String, Integer> pref : entry.getValue().entrySet()) {
                config.set(entry.getKey() + "." + pref.getKey(), pref.getValue());
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder playerprefs.yml : " + e.getMessage());
        }
    }

    public void setSlot(UUID uuid, String key, int slot) {
        prefs.computeIfAbsent(uuid, k -> new HashMap<>()).put(key, slot);
        save();
    }

    public int getSlot(UUID uuid, String key, int defaultSlot) {
        Map<String, Integer> playerPrefs = prefs.get(uuid);
        if (playerPrefs == null || !playerPrefs.containsKey(key)) return defaultSlot;
        return playerPrefs.get(key);
    }
}
