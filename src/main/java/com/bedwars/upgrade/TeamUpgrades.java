package com.bedwars.upgrade;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * État runtime (le temps d'une partie) des améliorations achetées par une équipe
 * au PNJ "Amélioration". Remis à zéro à chaque nouvelle partie (voir GameInstance).
 */
public class TeamUpgrades {

    public static final int MAX_LEVEL_SHARPENED_BLADES = 4;
    public static final int MAX_LEVEL_REINFORCED_ARMOR = 4;
    public static final int MAX_LEVEL_MANIAC_MINER = 2;
    public static final int MAX_LEVEL_FORGE = 4;
    public static final int MAX_TRAPS = 3;

    private int sharpenedBlades = 0;   // Force ajoutée aux armes
    private int reinforcedArmor = 0;   // Résistance ajoutée à l'armure
    private int maniacMiner = 0;       // Hâte sur sa propre base
    private int forge = 0;             // Vitesse des générateurs fer/or de l'équipe
    private boolean healPool = false;  // Régénération près du lit
    private boolean dragonBuff = false; // Un dragon supplémentaire protège la base

    private final Deque<TrapType> traps = new ArrayDeque<>();
    private long trapCooldownUntil = 0L; // System.currentTimeMillis(), anti-spam

    public int getSharpenedBlades() {
        return sharpenedBlades;
    }

    public boolean upgradeSharpenedBlades() {
        if (sharpenedBlades >= MAX_LEVEL_SHARPENED_BLADES) return false;
        sharpenedBlades++;
        return true;
    }

    public int getReinforcedArmor() {
        return reinforcedArmor;
    }

    public boolean upgradeReinforcedArmor() {
        if (reinforcedArmor >= MAX_LEVEL_REINFORCED_ARMOR) return false;
        reinforcedArmor++;
        return true;
    }

    public int getManiacMiner() {
        return maniacMiner;
    }

    public boolean upgradeManiacMiner() {
        if (maniacMiner >= MAX_LEVEL_MANIAC_MINER) return false;
        maniacMiner++;
        return true;
    }

    public int getForge() {
        return forge;
    }

    public boolean upgradeForge() {
        if (forge >= MAX_LEVEL_FORGE) return false;
        forge++;
        return true;
    }

    public boolean isHealPool() {
        return healPool;
    }

    public boolean buyHealPool() {
        if (healPool) return false;
        healPool = true;
        return true;
    }

    public boolean isDragonBuff() {
        return dragonBuff;
    }

    public boolean buyDragonBuff() {
        if (dragonBuff) return false;
        dragonBuff = true;
        return true;
    }

    public Deque<TrapType> getTraps() {
        return traps;
    }

    public boolean addTrap(TrapType type) {
        if (traps.size() >= MAX_TRAPS) return false;
        traps.addLast(type);
        return true;
    }

    /** Retire et retourne le prochain piège à déclencher, ou null s'il n'y en a pas / cooldown actif. */
    public TrapType pollTrap() {
        if (System.currentTimeMillis() < trapCooldownUntil) return null;
        TrapType type = traps.pollFirst();
        if (type != null) {
            trapCooldownUntil = System.currentTimeMillis() + 15_000L; // 15s avant le prochain déclenchement
        }
        return type;
    }
}
