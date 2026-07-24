package com.bedwars.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Sauvegarde et restauration d'une région rectangulaire de blocs, avec un système
 * de palette pour limiter la taille du fichier. Utilisé pour réinitialiser la map
 * d'une arène bedwars après chaque partie (blocs cassés/posés par les joueurs).
 */
public final class RegionSchematic {

    private RegionSchematic() {}

    public static void save(File file, Location pos1, Location pos2) throws IOException {
        World world = pos1.getWorld();
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;

        List<String> palette = new ArrayList<>();
        Map<String, Integer> paletteIndex = new HashMap<>();
        int[] indices = new int[sizeX * sizeY * sizeZ];

        int i = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    Block block = world.getBlockAt(x, y, z);
                    String data = block.getBlockData().getAsString();
                    Integer idx = paletteIndex.get(data);
                    if (idx == null) {
                        idx = palette.size();
                        palette.add(data);
                        paletteIndex.put(data, idx);
                    }
                    indices[i++] = idx;
                }
            }
        }

        file.getParentFile().mkdirs();
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(file)))) {
            out.writeUTF(world.getName());
            out.writeInt(minX);
            out.writeInt(minY);
            out.writeInt(minZ);
            out.writeInt(sizeX);
            out.writeInt(sizeY);
            out.writeInt(sizeZ);
            out.writeInt(palette.size());
            for (String s : palette) {
                out.writeUTF(s);
            }
            for (int idx : indices) {
                out.writeInt(idx);
            }
        }
    }

    public static void restore(File file, World world) throws IOException {
        if (!file.exists()) return;
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(file)))) {
            in.readUTF(); // nom du monde d'origine (on utilise le monde fourni en paramètre)
            int minX = in.readInt();
            int minY = in.readInt();
            int minZ = in.readInt();
            int sizeX = in.readInt();
            int sizeY = in.readInt();
            int sizeZ = in.readInt();
            int paletteSize = in.readInt();
            String[] palette = new String[paletteSize];
            for (int p = 0; p < paletteSize; p++) {
                palette[p] = in.readUTF();
            }
            BlockData[] paletteData = new BlockData[paletteSize];
            for (int p = 0; p < paletteSize; p++) {
                paletteData[p] = org.bukkit.Bukkit.createBlockData(palette[p]);
            }

            int i = 0;
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    for (int x = 0; x < sizeX; x++) {
                        int idx = in.readInt();
                        Block block = world.getBlockAt(minX + x, minY + y, minZ + z);
                        block.setBlockData(paletteData[idx], false);
                        i++;
                    }
                }
            }
        }
    }
}
