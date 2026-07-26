package com.bedwars.arena;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Color;

/**
 * Les 8 couleurs d'équipe possibles, dans l'ordre imposé :
 * Bleu, Rouge, Vert, Jaune, Rose, Violet, Magenta, Vert Foncé.
 * Seuls les nombres pairs d'équipes sont autorisés (2, 4, 6, 8),
 * on prend donc toujours les N premières couleurs de cette liste.
 */
public enum TeamColor {

    BLUE(1, "Bleu", ChatColor.BLUE, DyeColor.BLUE, Color.fromRGB(0x3971D6)),
    RED(2, "Rouge", ChatColor.RED, DyeColor.RED, Color.fromRGB(0xD63030)),
    GREEN(3, "Vert", ChatColor.GREEN, DyeColor.LIME, Color.fromRGB(0x4FD630)),
    YELLOW(4, "Jaune", ChatColor.YELLOW, DyeColor.YELLOW, Color.fromRGB(0xE9E93B)),
    PINK(5, "Rose", ChatColor.LIGHT_PURPLE, DyeColor.PINK, Color.fromRGB(0xEB7FBA)),
    PURPLE(6, "Violet", ChatColor.DARK_PURPLE, DyeColor.PURPLE, Color.fromRGB(0x8034C2)),
    MAGENTA(7, "Magenta", ChatColor.AQUA, DyeColor.MAGENTA, Color.fromRGB(0xC24FC2)),
    DARK_GREEN(8, "Vert Foncé", ChatColor.DARK_GREEN, DyeColor.GREEN, Color.fromRGB(0x2E7D32));

    private final int order;
    private final String displayName;
    private final ChatColor chatColor;
    private final DyeColor dyeColor;
    private final Color armorColor;

    TeamColor(int order, String displayName, ChatColor chatColor, DyeColor dyeColor, Color armorColor) {
        this.order = order;
        this.displayName = displayName;
        this.chatColor = chatColor;
        this.dyeColor = dyeColor;
        this.armorColor = armorColor;
    }

    public int getOrder() {
        return order;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatColor getChatColor() {
        return chatColor;
    }

    public DyeColor getDyeColor() {
        return dyeColor;
    }

    public Color getArmorColor() {
        return armorColor;
    }

    public String getColoredName() {
        return chatColor + displayName;
    }

    /**
     * Retourne les N premières couleurs (par ordre) pour un nombre d'équipes donné.
     * N doit être pair et compris entre 2 et 8.
     */
    public static TeamColor[] forTeamCount(int count) {
        TeamColor[] result = new TeamColor[count];
        TeamColor[] all = values();
        for (int i = 0; i < count; i++) {
            result[i] = all[i];
        }
        return result;
    }

    /**
     * Recherche une couleur d'équipe par son nom affiché (insensible à la casse et aux accents).
     */
    public static TeamColor fromDisplayName(String input) {
        if (input == null) return null;
        String normalized = normalize(input);
        for (TeamColor color : values()) {
            if (normalize(color.displayName).equals(normalized) || color.name().equalsIgnoreCase(input)) {
                return color;
            }
        }
        return null;
    }

    private static String normalize(String s) {
        return s.toLowerCase()
                .replace("é", "e")
                .replace("è", "e")
                .replace("ê", "e")
                .replace("à", "a")
                .replace(" ", "")
                .replace("_", "");
    }
}
