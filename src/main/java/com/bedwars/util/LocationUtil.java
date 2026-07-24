package com.bedwars.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public final class LocationUtil {

    private LocationUtil() {}

    public static void save(ConfigurationSection section, String path, Location location) {
        if (location == null || location.getWorld() == null) return;
        ConfigurationSection s = section.createSection(path);
        s.set("world", location.getWorld().getName());
        s.set("x", location.getX());
        s.set("y", location.getY());
        s.set("z", location.getZ());
        s.set("yaw", location.getYaw());
        s.set("pitch", location.getPitch());
    }

    public static Location load(ConfigurationSection section, String path) {
        ConfigurationSection s = section.getConfigurationSection(path);
        if (s == null) return null;
        String worldName = s.getString("world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        double x = s.getDouble("x");
        double y = s.getDouble("y");
        double z = s.getDouble("z");
        float yaw = (float) s.getDouble("yaw");
        float pitch = (float) s.getDouble("pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }
}
