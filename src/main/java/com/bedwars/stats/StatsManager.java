package com.bedwars.stats;

import com.bedwars.BedwarsPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Statistiques globales, persistées entre les parties (stats.yml) : victoires, kills, final
 * kills, lits détruits, parties jouées — pour les classements /bd leaderboard et /bd stats.
 */
public class StatsManager {

    public enum Stat { WINS, KILLS, FINAL_KILLS, BEDS_BROKEN, GAMES_PLAYED }

    public record PlayerStats(int gamesPlayed, int wins, int kills, int finalKills, int bedsBroken) {
        public static final PlayerStats EMPTY = new PlayerStats(0, 0, 0, 0, 0);
    }

    private final BedwarsPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerStats> stats = new HashMap<>();

    public StatsManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
    }

    public void load() {
        stats.clear();
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String uuidKey : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidKey);
                stats.put(uuid, new PlayerStats(
                        config.getInt(uuidKey + ".gamesPlayed", 0),
                        config.getInt(uuidKey + ".wins", 0),
                        config.getInt(uuidKey + ".kills", 0),
                        config.getInt(uuidKey + ".finalKills", 0),
                        config.getInt(uuidKey + ".bedsBroken", 0)));
            } catch (IllegalArgumentException ignored) {
                // clé invalide : on l'ignore simplement
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerStats> entry : stats.entrySet()) {
            String base = entry.getKey().toString();
            PlayerStats s = entry.getValue();
            config.set(base + ".gamesPlayed", s.gamesPlayed());
            config.set(base + ".wins", s.wins());
            config.set(base + ".kills", s.kills());
            config.set(base + ".finalKills", s.finalKills());
            config.set(base + ".bedsBroken", s.bedsBroken());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder stats.yml : " + e.getMessage());
        }
    }

    /** Enregistre les résultats d'un joueur pour une partie qui vient de se terminer. */
    public void recordGame(UUID uuid, boolean won, int kills, int finalKills, int bedsBroken) {
        PlayerStats current = stats.getOrDefault(uuid, PlayerStats.EMPTY);
        stats.put(uuid, new PlayerStats(
                current.gamesPlayed() + 1,
                current.wins() + (won ? 1 : 0),
                current.kills() + kills,
                current.finalKills() + finalKills,
                current.bedsBroken() + bedsBroken));
        save();
    }

    public PlayerStats getStats(UUID uuid) {
        return stats.getOrDefault(uuid, PlayerStats.EMPTY);
    }

    /** Top N joueurs pour une statistique donnée, du plus haut au plus bas. */
    public List<Map.Entry<UUID, Integer>> getTop(Stat stat, int limit) {
        List<Map.Entry<UUID, Integer>> entries = new ArrayList<>();
        for (Map.Entry<UUID, PlayerStats> entry : stats.entrySet()) {
            int value = valueFor(entry.getValue(), stat);
            if (value > 0) entries.add(Map.entry(entry.getKey(), value));
        }
        entries.sort(Comparator.<Map.Entry<UUID, Integer>>comparingInt(Map.Entry::getValue).reversed());
        return entries.size() > limit ? entries.subList(0, limit) : entries;
    }

    private int valueFor(PlayerStats s, Stat stat) {
        return switch (stat) {
            case WINS -> s.wins();
            case KILLS -> s.kills();
            case FINAL_KILLS -> s.finalKills();
            case BEDS_BROKEN -> s.bedsBroken();
            case GAMES_PLAYED -> s.gamesPlayed();
        };
    }

    public static String labelFor(Stat stat) {
        return switch (stat) {
            case WINS -> "Victoires";
            case KILLS -> "Kills";
            case FINAL_KILLS -> "Final Kills";
            case BEDS_BROKEN -> "Lits détruits";
            case GAMES_PLAYED -> "Parties jouées";
        };
    }
}
