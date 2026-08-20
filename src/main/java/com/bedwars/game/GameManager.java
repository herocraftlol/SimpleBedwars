package com.bedwars.game;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.ArenaState;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameManager {

    private final BedwarsPlugin plugin;
    private final Map<String, GameInstance> instances = new HashMap<>();
    /** Position de chaque joueur juste avant son /bd join, pour l'y renvoyer en fin de partie. */
    private final Map<UUID, Location> returnLocations = new HashMap<>();

    public GameManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    public GameInstance getInstance(Arena arena) {
        return instances.computeIfAbsent(arena.getName().toLowerCase(), k -> {
            arena.setState(ArenaState.WAITING);
            return new GameInstance(plugin, arena);
        });
    }

    public GameInstance getExistingInstance(Arena arena) {
        return instances.get(arena.getName().toLowerCase());
    }

    public void replaceInstance(Arena arena) {
        instances.put(arena.getName().toLowerCase(), new GameInstance(plugin, arena));
    }

    /** Retire définitivement l'instance de partie d'une arène (utilisé par /bd delete). */
    public void removeInstance(Arena arena) {
        instances.remove(arena.getName().toLowerCase());
    }

    public boolean joinArena(Player player, Arena arena) {
        if (!arena.isSaved()) return false;
        GameInstance instance = getInstance(arena);
        // On ne mémorise la position de départ que si elle n'est pas déjà suivie (évite d'écraser
        // la vraie position d'origine si le joueur enchaîne les parties sans jamais être renvoyé).
        returnLocations.putIfAbsent(player.getUniqueId(), player.getLocation());
        boolean joined = instance.addPlayer(player);
        if (!joined) returnLocations.remove(player.getUniqueId());
        return joined;
    }

    /** Téléporte un joueur là où il se trouvait juste avant son tout premier /bd join (sinon le spawn du monde). */
    public void returnPlayer(Player player) {
        Location loc = returnLocations.remove(player.getUniqueId());
        if (loc != null && loc.getWorld() != null) {
            player.teleport(loc);
        } else if (player.getWorld().getSpawnLocation() != null) {
            player.teleport(player.getWorld().getSpawnLocation());
        }
    }

    public GameInstance findInstanceOf(Player player) {
        for (GameInstance instance : instances.values()) {
            if (instance.getArena().getWaitingPlayers().contains(player.getUniqueId())
                    || instance.isAlivePlaying(player)
                    || instance.getArena().getSpectators().contains(player.getUniqueId())) {
                return instance;
            }
        }
        return null;
    }

    /** Fait quitter proprement un joueur de la partie/du lobby où il se trouve, et le renvoie où il était avant. */
    public boolean leave(Player player) {
        GameInstance instance = findInstanceOf(player);
        if (instance == null) return false;
        instance.handlePlayerLeave(player);
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.getInventory().clear();
        returnPlayer(player);
        return true;
    }

    public void shutdownAll() {
        instances.clear();
    }
}
