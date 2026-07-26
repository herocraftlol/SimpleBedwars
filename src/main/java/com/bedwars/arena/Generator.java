package com.bedwars.arena;

import org.bukkit.Location;

/**
 * Représente un point de spawn de minerai (fer, or, diamant, émeraude) sur une map.
 * Le tracking du nombre d'items encore au sol (pour le cap diamant/émeraude)
 * se fait à l'exécution dans GameInstance, pas ici.
 */
public class Generator {

    private final GeneratorType type;
    private final Location location;
    /** Couleur d'équipe associée si c'est un générateur "de base" d'équipe (fer/or), sinon null. */
    private TeamColor team;

    private long tickCounter = 0;

    public Generator(GeneratorType type, Location location) {
        this.type = type;
        this.location = location;
    }

    public GeneratorType getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public TeamColor getTeam() {
        return team;
    }

    public void setTeam(TeamColor team) {
        this.team = team;
    }

    public long getTickCounter() {
        return tickCounter;
    }

    public void incrementTick() {
        tickCounter++;
    }

    public void resetTick() {
        tickCounter = 0;
    }
}
