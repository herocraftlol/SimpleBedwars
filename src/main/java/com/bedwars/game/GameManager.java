package com.bedwars.game;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.ArenaState;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class GameManager {

    private final BedwarsPlugin plugin;
    private final Map<String, GameInstance> instances = new HashMap<>();

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
        return instance.addPlayer(player);
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

    public void shutdownAll() {
        instances.clear();
    }
}
