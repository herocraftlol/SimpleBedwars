package com.bedwars.util;

import com.bedwars.arena.TeamColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;

/**
 * Convertit n'importe quel bloc "coloré" (laine, terre cuite, béton, béton en poudre,
 * verre teinté, tapis, terre cuite vernissée...) vers l'équivalent de la couleur d'une
 * équipe, quelle que soit la couleur de base utilisée par l'admin en configurant le shop.
 *
 * Exemple : RED_WOOL configuré dans le shop -> devient BLUE_WOOL pour un acheteur de
 * l'équipe bleue, ORANGE_WOOL pour l'équipe orange, etc.
 */
public final class TeamColorUtil {

    private TeamColorUtil() {}

    /** Retourne le matériau recoloré à l'équipe donnée, ou le matériau d'origine s'il n'est pas "coloré". */
    public static Material colorize(Material base, TeamColor team) {
        if (base == null || team == null) return base;
        String name = base.name();
        for (DyeColor dc : DyeColor.values()) {
            String prefix = dc.name() + "_";
            if (!name.startsWith(prefix)) continue;
            String suffix = name.substring(prefix.length());
            Material candidate = Material.matchMaterial(team.getDyeColor().name() + "_" + suffix);
            if (candidate != null) return candidate;
        }
        return base;
    }

    /** Retourne true si ce matériau est une variante colorée reconnue (donc recolorable). */
    public static boolean isColorable(Material material) {
        if (material == null) return false;
        String name = material.name();
        for (DyeColor dc : DyeColor.values()) {
            if (name.startsWith(dc.name() + "_")) return true;
        }
        return false;
    }
}
