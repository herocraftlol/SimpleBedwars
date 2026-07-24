package com.bedwars.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;

import java.util.ArrayList;
import java.util.List;

public final class BedUtil {

    private BedUtil() {}

    public static boolean isBedBlock(Material material) {
        return material.name().endsWith("_BED");
    }

    /** Retourne les deux blocs (tête + pied) d'un lit, à partir de l'un ou l'autre bloc. */
    public static List<Block> getBedBlocks(Block anyBedBlock) {
        List<Block> result = new ArrayList<>();
        if (!(anyBedBlock.getBlockData() instanceof Bed bed)) {
            result.add(anyBedBlock);
            return result;
        }
        result.add(anyBedBlock);
        Block other = switch (bed.getPart()) {
            case HEAD -> anyBedBlock.getRelative(bed.getFacing().getOppositeFace());
            case FOOT -> anyBedBlock.getRelative(bed.getFacing());
        };
        result.add(other);
        return result;
    }

    public static boolean matchesBed(Location bedRefLocation, Block brokenBlock) {
        if (bedRefLocation == null || bedRefLocation.getWorld() == null) return false;
        Block ref = bedRefLocation.getWorld().getBlockAt(bedRefLocation);
        if (!isBedBlock(ref.getType())) return false;
        for (Block b : getBedBlocks(ref)) {
            if (b.getX() == brokenBlock.getX() && b.getY() == brokenBlock.getY() && b.getZ() == brokenBlock.getZ()
                    && b.getWorld().equals(brokenBlock.getWorld())) {
                return true;
            }
        }
        return false;
    }
}
