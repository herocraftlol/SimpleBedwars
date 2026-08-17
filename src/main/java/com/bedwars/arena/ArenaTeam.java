package com.bedwars.arena;

import org.bukkit.Location;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * État d'une équipe au sein d'une arène (config + état runtime pendant une partie).
 */
public class ArenaTeam {

    private final TeamColor color;
    private final int maxPlayers;

    // Configuration (persistée)
    private Location bedLocation;
    private Location spawnLocation;
    private Location shopLocation;
    private Location upgradeLocation;

    // État runtime (partie en cours)
    private boolean bedDestroyed = false;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final Set<UUID> alivePlayers = new LinkedHashSet<>();

    public ArenaTeam(TeamColor color, int maxPlayers) {
        this.color = color;
        this.maxPlayers = maxPlayers;
    }

    public TeamColor getColor() {
        return color;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public Location getBedLocation() {
        return bedLocation;
    }

    public void setBedLocation(Location bedLocation) {
        this.bedLocation = bedLocation;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public Location getShopLocation() {
        return shopLocation;
    }

    public void setShopLocation(Location shopLocation) {
        this.shopLocation = shopLocation;
    }

    public Location getUpgradeLocation() {
        return upgradeLocation;
    }

    public void setUpgradeLocation(Location upgradeLocation) {
        this.upgradeLocation = upgradeLocation;
    }

    public boolean isBedDestroyed() {
        return bedDestroyed;
    }

    public void setBedDestroyed(boolean bedDestroyed) {
        this.bedDestroyed = bedDestroyed;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public Set<UUID> getAlivePlayers() {
        return alivePlayers;
    }

    public boolean isFull() {
        return members.size() >= maxPlayers;
    }

    public boolean isEliminated() {
        return bedDestroyed && alivePlayers.isEmpty();
    }

    public boolean isFullyConfigured() {
        return bedLocation != null && spawnLocation != null
                && shopLocation != null && upgradeLocation != null;
    }

    /** Réinitialise l'état runtime pour une nouvelle partie (garde la config). */
    public void resetRuntime() {
        bedDestroyed = false;
        members.clear();
        alivePlayers.clear();
    }
}
