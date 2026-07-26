package com.bedwars.util;

import com.bedwars.BedwarsPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

/**
 * Construit une cage de blocs invisibles (barrières) bien centrée sur l'emplacement
 * où apparaissent les spectateurs, pour matérialiser visuellement la zone. Comme les
 * spectateurs sont en mode Spectateur (vol/traversée des blocs), le confinement réel
 * est complété par une vérification de mouvement (voir PlayerProtectionListener) ;
 * cette cage sert avant tout de repère visuel autour du point de spectateur.
 */
public final class SpectatorCageManager {

    private SpectatorCageManager() {}

    public static void build(BedwarsPlugin plugin, Location center) {
        if (center == null || center.getWorld() == null) return;
        int radius = plugin.getConfig().getInt("spectator.cage-radius", 4);
        int height = plugin.getConfig().getInt("spectator.cage-height", 4);

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                for (int y = cy - 1; y <= cy + height; y++) {
                    boolean edge = x == cx - radius || x == cx + radius
                            || z == cz - radius || z == cz + radius
                            || y == cy - 1 || y == cy + height;
                    if (!edge) continue; // ne construit que la coquille (murs/sol/plafond)
                    Block block = center.getWorld().getBlockAt(x, y, z);
                    if (block.getType() == Material.AIR) {
                        block.setType(Material.BARRIER, false);
                    }
                }
            }
        }
    }
}
