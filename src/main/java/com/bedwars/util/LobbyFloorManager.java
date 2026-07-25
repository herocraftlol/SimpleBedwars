package com.bedwars.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Construit une plateforme de blocs invisibles (barrières) couvrant la zone d'attente
 * (lobbyPos1/lobbyPos2), pour que les joueurs en lobby voient la map en dessous d'eux.
 * Retirée dès que la partie démarre.
 */
public final class LobbyFloorManager {

    private LobbyFloorManager() {}

    public static List<Block> build(Location pos1, Location pos2) {
        List<Block> placed = new ArrayList<>();
        if (pos1 == null || pos2 == null || pos1.getWorld() == null) return placed;

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        int y = Math.min(pos1.getBlockY(), pos2.getBlockY());

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Block block = pos1.getWorld().getBlockAt(x, y, z);
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
