package com.bedwars.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Construit une plateforme de blocs invisibles (barrières) flottant au-dessus de la
 * map (centrée automatiquement au-dessus de la zone de jeu), pour que les joueurs en
 * attente voient la map en dessous d'eux pendant que tout le monde se connecte.
 * Retirée dès que la partie démarre.
 */
public final class LobbyFloorManager {

    private LobbyFloorManager() {}

    /** Construit une plateforme carrée (2*radius+1 de côté) centrée sur "center". */
    public static List<Block> build(Location center, int radius) {
        List<Block> placed = new ArrayList<>();
        if (center == null || center.getWorld() == null) return placed;

        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        int y = center.getBlockY();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                Block block = center.getWorld().getBlockAt(x, y, z);
                if (block.getType() == Material.AIR) {
                    block.setType(Material.BARRIER, false);
                    placed.add(block);
                }
            }
        }
        return placed;
    }

    public static void remove(List<Block> placed) {
        for (Block block : placed) {
            if (block.getType() == Material.BARRIER) {
                block.setType(Material.AIR, false);
            }
        }
    }
}
