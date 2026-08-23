package com.bedwars.stats;

import com.bedwars.BedwarsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Hologrammes de classement invocables en jeu (façon HikaBrain) : top lits détruits, top
 * victoires, top kills, top final kills, top parties jouées. Un hologramme par catégorie,
 * persisté (leaderboards.yml), rafraîchi automatiquement toutes les 30 secondes.
 */
public class LeaderboardManager {

    private static final double LINE_GAP = 0.27;
    private static final long REFRESH_INTERVAL_TICKS = 20L * 30;
    private static final int TOP_SIZE = 10;

    private final BedwarsPlugin plugin;
    private final File file;
    private final Map<StatsManager.Stat, Location> locations = new EnumMap<>(StatsManager.Stat.class);
    private final Map<StatsManager.Stat, List<UUID>> lineEntities = new EnumMap<>(StatsManager.Stat.class);
    private BukkitTask refreshTask;

    public LeaderboardManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "leaderboards.yml");
        for (StatsManager.Stat stat : StatsManager.Stat.values()) {
            lineEntities.put(stat, new ArrayList<>());
        }
    }

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("locations");
        if (root == null) return;

        for (StatsManager.Stat stat : StatsManager.Stat.values()) {
            ConfigurationSection s = root.getConfigurationSection(stat.name());
            if (s == null) continue;
            World world = Bukkit.getWorld(s.getString("world", ""));
            if (world == null) continue;
            Location loc = new Location(world, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"));
            locations.put(stat, loc);
        }
        // Les hologrammes eux-mêmes ne survivent pas à un redémarrage (non persistants) : on les
        // refait apparaître ici pour chaque emplacement sauvegardé.
        for (StatsManager.Stat stat : locations.keySet()) {
            spawnHologram(stat, locations.get(stat));
        }
        startAutoRefresh();
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<StatsManager.Stat, Location> entry : locations.entrySet()) {
            String base = "locations." + entry.getKey().name();
            Location loc = entry.getValue();
            config.set(base + ".world", loc.getWorld().getName());
            config.set(base + ".x", loc.getX());
            config.set(base + ".y", loc.getY());
            config.set(base + ".z", loc.getZ());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder leaderboards.yml : " + e.getMessage());
        }
    }

    /** Invoque (ou déplace) le leaderboard d'une catégorie à l'emplacement donné. */
    public void summon(StatsManager.Stat stat, Location location) {
        removeHologram(stat);
        Location anchor = location.getBlock().getLocation().add(0.5, 1.0, 0.5);
        locations.put(stat, anchor);
        spawnHologram(stat, anchor);
        save();
        startAutoRefresh();
    }

    public boolean remove(StatsManager.Stat stat) {
        boolean existed = locations.containsKey(stat);
        removeHologram(stat);
        locations.remove(stat);
        save();
        return existed;
    }

    private void startAutoRefresh() {
        if (refreshTask != null) return;
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (StatsManager.Stat stat : new ArrayList<>(locations.keySet())) {
                spawnHologram(stat, locations.get(stat));
            }
        }, REFRESH_INTERVAL_TICKS, REFRESH_INTERVAL_TICKS);
    }

    private void removeHologram(StatsManager.Stat stat) {
        for (UUID id : lineEntities.get(stat)) {
            org.bukkit.entity.Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        lineEntities.get(stat).clear();
    }

    private void spawnHologram(StatsManager.Stat stat, Location anchor) {
        removeHologram(stat);
        List<String> lines = buildLines(stat);

        // On empile les lignes du haut vers le bas, à partir de l'ancre.
        double y = anchor.getY() + (lines.size() - 1) * LINE_GAP;
        for (String line : lines) {
            Location loc = new Location(anchor.getWorld(), anchor.getX(), y, anchor.getZ());
            ArmorStand stand = (ArmorStand) anchor.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setCustomNameVisible(true);
            stand.setCustomName(line);
            lineEntities.get(stat).add(stand.getUniqueId());
            y -= LINE_GAP;
        }
    }

    private List<String> buildLines(StatsManager.Stat stat) {
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GOLD + "" + ChatColor.BOLD + "▬ TOP " + StatsManager.labelFor(stat).toUpperCase() + " ▬");

        List<Map.Entry<UUID, Integer>> top = plugin.getStatsManager().getTop(stat, TOP_SIZE);
        if (top.isEmpty()) {
            lines.add(ChatColor.GRAY + "Aucune donnée pour le moment");
            return lines;
        }

        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : top) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = "???";
            ChatColor rankColor = switch (rank) {
                case 1 -> ChatColor.GOLD;
                case 2 -> ChatColor.GRAY;
                case 3 -> ChatColor.DARK_RED;
                default -> ChatColor.WHITE;
            };
            lines.add(rankColor + "#" + rank + " " + ChatColor.YELLOW + name + ChatColor.GRAY + " - " + ChatColor.WHITE + entry.getValue());
            rank++;
        }
        return lines;
    }
}
