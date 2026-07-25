package com.bedwars.shop;

import java.util.HashMap;
import java.util.Map;

public class UpgradeTypeConfig {

    private final UpgradeType type;
    private int maxLevel;
    private final Map<Integer, Integer> levelPrices = new HashMap<>(); // niveau -> prix en diamants

    public UpgradeTypeConfig(UpgradeType type) {
        this.type = type;
        this.maxLevel = type.getDefaultMaxLevel();
    }

    public UpgradeType getType() {
        return type;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        int cap = type.getDefaultMaxLevel();
        this.maxLevel = Math.max(1, Math.min(maxLevel, cap));
    }

    public int getPriceForLevel(int level) {
        return levelPrices.getOrDefault(level, defaultPriceForLevel(level));
    }

    public void setPriceForLevel(int level, int price) {
        levelPrices.put(level, price);
    }

    public Map<Integer, Integer> getLevelPrices() {
        return levelPrices;
    }

    private int defaultPriceForLevel(int level) {
        // Prix par défaut raisonnable si l'admin n'a rien configuré : augmente avec le niveau.
        return type.isTrap() ? 1 + (level - 1) : level * 2;
    }
}
