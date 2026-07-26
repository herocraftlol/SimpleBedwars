package com.bedwars.arena;

import com.bedwars.shop.UpgradeType;
import org.bukkit.Location;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
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

    // Améliorations d'équipe (réinitialisées à chaque partie)
    private final Map<UpgradeType, Integer> upgradeLevels = new EnumMap<>(UpgradeType.class);
    private final Map<UpgradeType, Boolean> trapArmed = new EnumMap<>(UpgradeType.class);

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

    public int getUpgradeLevel(UpgradeType type) {
        if (!type.isLeveled()) {
            return upgradeLevels.getOrDefault(type, 0); // 0 = pas acheté, 1 = acheté (Heal/Dragon)
        }
        return upgradeLevels.getOrDefault(type, 1); // les upgrades à palier démarrent à 1 (base)
    }

    public void setUpgradeLevel(UpgradeType type, int level) {
        upgradeLevels.put(type, level);
    }

    public boolean isTrapArmed(UpgradeType type) {
        return trapArmed.getOrDefault(type, false);
    }

    public void setTrapArmed(UpgradeType type, boolean armed) {
        trapArmed.put(type, armed);
    }

    /** Réinitialise l'état runtime pour une nouvelle partie (garde la config). */
    public void resetRuntime() {
        bedDestroyed = false;
        members.clear();
        alivePlayers.clear();
        upgradeLevels.clear();
        trapArmed.clear();
    }
}
