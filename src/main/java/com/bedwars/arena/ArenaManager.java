package com.bedwars.arena;

import com.bedwars.BedwarsPlugin;
import com.bedwars.util.LocationUtil;
import com.bedwars.util.RegionSchematic;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ArenaManager {

    private final BedwarsPlugin plugin;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private final File arenasFolder;

    public ArenaManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
        this.arenasFolder = new File(plugin.getDataFolder(), "arenas");
        if (!arenasFolder.exists()) arenasFolder.mkdirs();
    }

    public Arena createArena(String name) {
        if (arenas.containsKey(name.toLowerCase())) return null;
        Arena arena = new Arena(name);
        arenas.put(name.toLowerCase(), arena);
        saveArena(arena);
        return arena;
    }

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public Map<String, Arena> getArenas() {
        return arenas;
    }

    public File getRegionFile(Arena arena) {
        return new File(arenasFolder, arena.getName().toLowerCase() + "_region.dat");
    }

    /** Capture la zone de jeu configurée (pos1/pos2) dans un fichier de blocs, appelé lors du /bd <nom> save. */
    public void captureRegion(Arena arena) {
        if (arena.getGamePos1() == null || arena.getGamePos2() == null) return;
        try {
            RegionSchematic.save(getRegionFile(arena), arena.getGamePos1(), arena.getGamePos2());
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder la région de l'arène " + arena.getName() + ": " + e.getMessage());
        }
    }

    /** Restaure la map à son état d'origine (fin de partie). */
    public void restoreRegion(Arena arena) {
        File file = getRegionFile(arena);
        if (!file.exists() || arena.getGamePos1() == null) return;
        try {
            RegionSchematic.restore(file, arena.getGamePos1().getWorld());
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de restaurer la région de l'arène " + arena.getName() + ": " + e.getMessage());
        }
    }

    public void saveArena(Arena arena) {
        File file = new File(arenasFolder, arena.getName().toLowerCase() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("name", arena.getName());
        config.set("teamCount", arena.getTeamCount());
        config.set("playersPerTeam", arena.getPlayersPerTeam());
        config.set("saved", arena.isSaved());
        config.set("gameZoneConfirmed", arena.isGameZoneConfirmed());

        LocationUtil.save(config, "gamePos1", arena.getGamePos1());
        LocationUtil.save(config, "gamePos2", arena.getGamePos2());
        LocationUtil.save(config, "spec", arena.getSpecLocation());

        for (TeamColor color : TeamColor.values()) {
            ArenaTeam team = arena.getTeams().get(color);
            if (team == null) continue;
            String base = "teams." + color.name();
            LocationUtil.save(config, base + ".bed", team.getBedLocation());
            LocationUtil.save(config, base + ".spawn", team.getSpawnLocation());
            LocationUtil.save(config, base + ".shop", team.getShopLocation());
            LocationUtil.save(config, base + ".upgrade", team.getUpgradeLocation());
        }

        int gi = 0;
        for (Generator gen : arena.getGenerators()) {
            String base = "generators." + gi;
            config.set(base + ".type", gen.getType().name());
            LocationUtil.save(config, base + ".location", gen.getLocation());
            if (gen.getTeam() != null) config.set(base + ".team", gen.getTeam().name());
            gi++;
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder l'arène " + arena.getName() + ": " + e.getMessage());
        }
    }

    public void loadAll() {
        File[] files = arenasFolder.listFiles((dir, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            try {
                loadArena(file);
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur de chargement de l'arène depuis " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    private void loadArena(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String name = config.getString("name");
        if (name == null) return;
        Arena arena = new Arena(name);
        arena.setTeamCount(config.getInt("teamCount", 0));
        arena.setPlayersPerTeam(config.getInt("playersPerTeam", 0));
        arena.setSaved(config.getBoolean("saved", false));
        arena.setGameZoneConfirmed(config.getBoolean("gameZoneConfirmed", false));

        arena.setGamePos1(LocationUtil.load(config, "gamePos1"));
        arena.setGamePos2(LocationUtil.load(config, "gamePos2"));
        arena.setSpecLocation(LocationUtil.load(config, "spec"));

        if (config.isConfigurationSection("teams")) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("teams")).getKeys(false)) {
                TeamColor color;
                try {
                    color = TeamColor.valueOf(key);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                ArenaTeam team = arena.getOrCreateTeam(color);
                String base = "teams." + key;
                team.setBedLocation(LocationUtil.load(config, base + ".bed"));
                team.setSpawnLocation(LocationUtil.load(config, base + ".spawn"));
                team.setShopLocation(LocationUtil.load(config, base + ".shop"));
                team.setUpgradeLocation(LocationUtil.load(config, base + ".upgrade"));
            }
        }

        if (config.isConfigurationSection("generators")) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("generators")).getKeys(false)) {
                String base = "generators." + key;
                String typeName = config.getString(base + ".type");
                GeneratorType type = typeName != null ? GeneratorType.valueOf(typeName) : null;
                Location loc = LocationUtil.load(config, base + ".location");
                if (type == null || loc == null) continue;
                Generator gen = new Generator(type, loc);
                String teamName = config.getString(base + ".team");
                if (teamName != null) gen.setTeam(TeamColor.valueOf(teamName));
                arena.getGenerators().add(gen);
            }
        }

        arena.setState(ArenaState.SETUP);
        arenas.put(name.toLowerCase(), arena);
    }

    /**
     * Supprime intégralement une arène : fichier de configuration, fichier de région
     * sauvegardée, et entrée en mémoire. Ne s'occupe pas des NPC / instances de partie
     * en cours : cela doit être géré par l'appelant (voir BedwarsCommand#handleDeleteConfirm).
     */
    public void deleteArena(Arena arena) {
        arenas.remove(arena.getName().toLowerCase());

        File configFile = new File(arenasFolder, arena.getName().toLowerCase() + ".yml");
        if (configFile.exists() && !configFile.delete()) {
            plugin.getLogger().warning("Impossible de supprimer le fichier " + configFile.getName());
        }

        File regionFile = getRegionFile(arena);
        if (regionFile.exists() && !regionFile.delete()) {
            plugin.getLogger().warning("Impossible de supprimer le fichier " + regionFile.getName());
        }
    }
}
