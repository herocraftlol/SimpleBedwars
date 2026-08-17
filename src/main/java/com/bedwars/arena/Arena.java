package com.bedwars.arena;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Arena {

    private final String name;
    private ArenaState state = ArenaState.SETUP;

    private int teamCount = 0;       // 2, 4, 6 ou 8
    private int playersPerTeam = 0;  // 2 à 16

    private final Map<TeamColor, ArenaTeam> teams = new EnumMap<>(TeamColor.class);
    private final List<Generator> generators = new ArrayList<>();

    // Zone de jeu (façon WorldEdit)
    private Location gamePos1;
    private Location gamePos2;
    private boolean gameZoneConfirmed = false;

    // Spawn des spectateurs / centre du lobby d'attente.
    // Une cage invisible (voir WaitingLobbyManager) est construite automatiquement
    // au-dessus de la map, centrée sur cet emplacement, tant que des joueurs attendent.
    private Location specLocation;

    private boolean saved = false; // true = configuration validée par /bd <nom> save

    // État runtime (rempli par GameInstance)
    private final List<UUID> waitingPlayers = new ArrayList<>();
    private final List<UUID> spectators = new ArrayList<>();

    public Arena(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public ArenaState getState() {
        return state;
    }

    public void setState(ArenaState state) {
        this.state = state;
    }

    public int getTeamCount() {
        return teamCount;
    }

    public void setTeamCount(int teamCount) {
        this.teamCount = teamCount;
    }

    public int getPlayersPerTeam() {
        return playersPerTeam;
    }

    public void setPlayersPerTeam(int playersPerTeam) {
        this.playersPerTeam = playersPerTeam;
    }

    public Map<TeamColor, ArenaTeam> getTeams() {
        return teams;
    }

    public List<Generator> getGenerators() {
        return generators;
    }

    public Location getGamePos1() {
        return gamePos1;
    }

    public void setGamePos1(Location gamePos1) {
        this.gamePos1 = gamePos1;
    }

    public Location getGamePos2() {
        return gamePos2;
    }

    public void setGamePos2(Location gamePos2) {
        this.gamePos2 = gamePos2;
    }

    public boolean isGameZoneConfirmed() {
        return gameZoneConfirmed;
    }

    public void setGameZoneConfirmed(boolean gameZoneConfirmed) {
        this.gameZoneConfirmed = gameZoneConfirmed;
    }

    public Location getSpecLocation() {
        return specLocation;
    }

    public void setSpecLocation(Location specLocation) {
        this.specLocation = specLocation;
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public List<UUID> getWaitingPlayers() {
        return waitingPlayers;
    }

    public List<UUID> getSpectators() {
        return spectators;
    }

    public int getMaxPlayers() {
        return teamCount * playersPerTeam;
    }

    public int getCurrentPlayerCount() {
        return waitingPlayers.size();
    }

    public boolean isLobbyFull() {
        return teamCount > 0 && getCurrentPlayerCount() >= getMaxPlayers();
    }

    /**
     * Vérifie que tout ce qui est nécessaire est configuré, et retourne
     * la liste des éléments manquants (vide = tout est bon).
     */
    public List<String> getMissingRequirements() {
        List<String> missing = new ArrayList<>();
        if (teamCount <= 0) missing.add("nombre d'équipes (/bd " + name + " equipe <nb> <joueurs>)");
        if (!gameZoneConfirmed) missing.add("zone de jeu (/bd " + name + " pos1/pos2/posconfirm)");
        if (specLocation == null) missing.add("centre du lobby d'attente / spawn spectateurs (/bd " + name + " spec)");

        if (teamCount > 0) {
            for (TeamColor color : TeamColor.forTeamCount(teamCount)) {
                ArenaTeam team = teams.get(color);
                if (team == null || team.getBedLocation() == null) {
                    missing.add("lit équipe " + color.getColoredName());
                }
                if (team == null || team.getSpawnLocation() == null) {
                    missing.add("spawn équipe " + color.getColoredName());
                }
                if (team == null || team.getShopLocation() == null) {
                    missing.add("shop équipe " + color.getColoredName());
                }
                if (team == null || team.getUpgradeLocation() == null) {
                    missing.add("upgrade équipe " + color.getColoredName());
                }
            }
        }

        boolean hasIron = generators.stream().anyMatch(g -> g.getType() == GeneratorType.FER);
        boolean hasGold = generators.stream().anyMatch(g -> g.getType() == GeneratorType.OR);
        boolean hasDiamond = generators.stream().anyMatch(g -> g.getType() == GeneratorType.DIAMOND);
        boolean hasEmerald = generators.stream().anyMatch(g -> g.getType() == GeneratorType.EMERAUDE);
        if (!hasIron) missing.add("au moins un générateur de fer");
        if (!hasGold) missing.add("au moins un générateur d'or");
        if (!hasDiamond) missing.add("au moins un générateur de diamant");
        if (!hasEmerald) missing.add("au moins un générateur d'émeraude");

        return missing;
    }

    public ArenaTeam getOrCreateTeam(TeamColor color) {
        return teams.computeIfAbsent(color, c -> new ArenaTeam(c, playersPerTeam));
    }

    /** Réinitialise l'état runtime de l'arène (fin de partie -> retour en lobby). */
    public void resetRuntime() {
        waitingPlayers.clear();
        spectators.clear();
        for (ArenaTeam team : teams.values()) {
            team.resetRuntime();
        }
        for (Generator g : generators) {
            g.resetTick();
        }
        state = ArenaState.WAITING;
    }
}
