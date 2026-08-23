package com.bedwars.util;

import com.bedwars.BedwarsPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Les articles "favoris" de chaque joueur, affichés dans l'onglet "Quick Buy" (nether star, en
 * haut à gauche du shop, onglet par défaut à l'ouverture) — un peu à la Hypixel. Chaque favori est
 * référencé par une petite chaîne stable (ex: "blocks:9" pour un article configuré au slot 9 de la
 * catégorie "blocks", ou "special_potions:0" pour un article spécial comme les potions).
 */
public class FavoritesManager {

    private static final int MAX_FAVORITES = 28;

    private final BedwarsPlugin plugin;
    private final File file;
    private final Map<UUID, List<String>> favorites = new HashMap<>();

    public FavoritesManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "favorites.yml");
    }

    public void load() {
        favorites.clear();
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String uuidKey : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidKey);
                favorites.put(uuid, new ArrayList<>(config.getStringList(uuidKey)));
            } catch (IllegalArgumentException ignored) {
                // clé invalide : ignorée
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, List<String>> entry : favorites.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder favorites.yml : " + e.getMessage());
        }
    }

    public List<String> getFavorites(UUID uuid) {
        return favorites.getOrDefault(uuid, List.of());
    }

    public boolean isFavorite(UUID uuid, String ref) {
        return favorites.getOrDefault(uuid, List.of()).contains(ref);
    }

    /** Ajoute/retire une référence des favoris. Retourne true si elle vient d'être ajoutée. */
    public boolean toggle(UUID uuid, String ref) {
        List<String> list = favorites.computeIfAbsent(uuid, k -> new ArrayList<>());
        boolean added;
        if (list.contains(ref)) {
            list.remove(ref);
            added = false;
        } else {
            if (list.size() >= MAX_FAVORITES) {
                list.remove(0); // le plus ancien cède la place, pour ne jamais bloquer l'ajout
            }
            list.add(ref);
            added = true;
        }
        save();
        return added;
    }
}
