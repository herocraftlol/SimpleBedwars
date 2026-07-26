package com.bedwars.shop;

/**
 * Les améliorations d'équipe achetables au villageois "Upgrade".
 * Les clés entre parenthèses correspondent à ce qui est tapé dans la commande
 * (/bd shop Upgrade custom ... item ... "Sharp" ...), le label est ce qui
 * s'affiche sur l'item et dans le chat.
 */
public enum UpgradeType {
    HEAL("Heal", "Heal Boost", false, 1),
    SHARP("Sharp", "Sharpness", true, 5),
    ARMOR("Armur", "Armure", true, 5),
    MANIAC("maniac", "Maniac Mineur", true, 5),
    CHUTE("chute", "Dégât de chute", true, 5),
    FORGE("Forge", "Forge", true, 5),
    TRAP_ALARM("TrapA", "Alarme", true, 2),
    TRAP_BLIND("TrapC", "Piège de cécité", true, 2),
    TRAP_MINER("TrapM", "Piège du Mineur", true, 2),
    DRAGON("Dragon", "Dragon Buff", false, 1);

    private final String commandKey;
    private final String label;
    private final boolean leveled;
    private final int defaultMaxLevel;

    UpgradeType(String commandKey, String label, boolean leveled, int defaultMaxLevel) {
        this.commandKey = commandKey;
        this.label = label;
        this.leveled = leveled;
        this.defaultMaxLevel = defaultMaxLevel;
    }

    public String getCommandKey() {
        return commandKey;
    }

    public String getLabel() {
        return label;
    }

    /** false pour Heal Boost et Dragon Buff : achat unique, pas de palier. */
    public boolean isLeveled() {
        return leveled;
    }

    public boolean isTrap() {
        return this == TRAP_ALARM || this == TRAP_BLIND || this == TRAP_MINER;
    }

    public int getDefaultMaxLevel() {
        return defaultMaxLevel;
    }

    public static UpgradeType fromCommandKey(String input) {
        if (input == null) return null;
        for (UpgradeType type : values()) {
            if (type.commandKey.equalsIgnoreCase(input) || type.name().equalsIgnoreCase(input)) {
                return type;
            }
        }
        return null;
    }
}
