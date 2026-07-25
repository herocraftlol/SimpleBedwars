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

    /** Supprime entièrement une arène (toute la configuration + les fichiers). */
    public boolean deleteArena(String name) {
        Arena arena = arenas.remove(name.toLowerCase());
        if (arena == null) return false;
        File configFile = new File(arenasFolder, name.toLowerCase() + ".yml");
        File regionFile = getRegionFile(arena);
        if (configFile.exists()) configFile.delete();
        if (regionFile.exists()) regionFile.delete();
        return true;
    }

    /**
     * Supprime l'élément de configuration correspondant exactement à l'emplacement donné
     * (lit, spawn, shop, upgrade, spec, générateur...). Retourne une description de ce qui
     * a été supprimé, ou null si rien ne correspondait à cet emplacement.
     */
    public String deleteAtLocation(Arena arena, Location location) {
        if (arena.getSpecLocation() != null && sameBlock(arena.getSpecLocation(), location)) {
            arena.setSpecLocation(null);
            return "spawn des spectateurs";
        }
        for (TeamColor color : TeamColor.values()) {
            ArenaTeam team = arena.getTeams().get(color);
            if (team == null) continue;
            if (team.getBedLocation() != null && sameBlock(team.getBedLocation(), location)) {
                team.setBedLocation(null);
                return "lit de l'équipe " + color.getDisplayName();
            }
            if (team.getSpawnLocation() != null && sameBlock(team.getSpawnLocation(), location)) {
                team.setSpawnLocation(null);
                return "spawn de l'équipe " + color.getDisplayName();
            }
            if (team.getShopLocation() != null && sameBlock(team.getShopLocation(), location)) {
                team.setShopLocation(null);
                return "shop de l'équipe " + color.getDisplayName();
            }
            if (team.getUpgradeLocation() != null && sameBlock(team.getUpgradeLocation(), location)) {
                team.setUpgradeLocation(null);
                return "upgrade de l'équipe " + color.getDisplayName();
            }
        }
        Generator toRemove = null;
        for (Generator gen : arena.getGenerators()) {
            if (sameBlock(gen.getLocation(), location)) {
                toRemove = gen;
                break;
            }
        }
        if (toRemove != null) {
            arena.getGenerators().remove(toRemove);
            return "générateur de " + toRemove.getType().name().toLowerCase();
        }
        if (arena.getGamePos1() != null && sameBlock(arena.getGamePos1(), location)) {
            arena.setGamePos1(null);
            arena.setGameZoneConfirmed(false);
            return "position 1 de la zone de jeu";
        }
        if (arena.getGamePos2() != null && sameBlock(arena.getGamePos2(), location)) {
            arena.setGamePos2(null);
            arena.setGameZoneConfirmed(false);
            return "position 2 de la zone de jeu";
        }
        if (arena.getLobbyPos1() != null && sameBlock(arena.getLobbyPos1(), location)) {
            arena.setLobbyPos1(null);
            arena.setLobbyZoneConfirmed(false);
            return "position 1 de la zone d'attente";
        }
        if (arena.getLobbyPos2() != null && sameBlock(arena.getLobbyPos2(), location)) {
            arena.setLobbyPos2(null);
            arena.setLobbyZoneConfirmed(false);
            return "position 2 de la zone d'attente";
        }
        return null;
    }

    private boolean sameBlock(Location a, Location b) {
        if (a == null || b == null || a.getWorld() == null || b.getWorld() == null) return false;
        return a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
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
        config.set("lobbyZoneConfirmed", arena.isLobbyZoneConfirmed());

        LocationUtil.save(config, "gamePos1", arena.getGamePos1());
        LocationUtil.save(config, "gamePos2", arena.getGamePos2());
        LocationUtil.save(config, "lobbyPos1", arena.getLobbyPos1());
        LocationUtil.save(config, "lobbyPos2", arena.getLobbyPos2());
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
        arena.setLobbyZoneConfirmed(config.getBoolean("lobbyZoneConfirmed", false));

        arena.setGamePos1(LocationUtil.load(config, "gamePos1"));
        arena.setGamePos2(LocationUtil.load(config, "gamePos2"));
        arena.setLobbyPos1(LocationUtil.load(config, "lobbyPos1"));
        arena.setLobbyPos2(LocationUtil.load(config, "lobbyPos2"));
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
}
