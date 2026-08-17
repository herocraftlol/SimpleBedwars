package com.bedwars.upgrade;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.List;

/**
 * Les pièges achetables à l'amélioration, façon Hypixel Bedwars.
 * Une équipe peut avoir jusqu'à 3 pièges en attente, déclenchés automatiquement
 * quand un ennemi entre dans la zone de base (voir GameInstance#checkTraps).
 */
public enum TrapType {

    ALARM("Piège d'alarme", Material.TRIPWIRE_HOOK, 1,
            List.of("Révèle et alerte des",
                    "ennemis qui entrent",
                    "dans votre base.")),
    COUNTER_OFFENSIVE("Contre-attaque", Material.FEATHER, 2,
            List.of("Donne Vitesse II et",
                    "Sauteur II à votre équipe",
                    "pendant 8 secondes.")),
    ITS_A_TRAP("C'est un piège !", Material.STICKY_PISTON, 2,
            List.of("Inflige Faiblesse et", "Lenteur II à l'ennemi", "pendant 8 secondes.")),
    MINER_FATIGUE("Fatigue du mineur", Material.TNT, 3,
            List.of("Inflige Fatigue du", "mineur III à tous les",
                    "ennemis proches pendant", "10 secondes."));

    private final String displayName;
    private final Material icon;
    private final int priceEmeralds;
    private final List<String> description;

    TrapType(String displayName, Material icon, int priceEmeralds, List<String> description) {
        this.displayName = displayName;
        this.icon = icon;
        this.priceEmeralds = priceEmeralds;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public int getPriceEmeralds() {
        return priceEmeralds;
    }

    public List<String> getDescription() {
        return description;
    }

    public String getColoredName() {
        return ChatColor.YELLOW + displayName;
    }
}
