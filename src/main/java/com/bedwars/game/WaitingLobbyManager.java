package com.bedwars.game;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;

/**
 * Construit et détruit, à la demande, une cage invisible (blocs BARRIER, invisibles
 * en jeu mais solides) centrée sur l'emplacement défini par /bd <nom> spec.
 * Cette cage matérialise le "lobby d'attente" flottant au-dessus de la map :
 * les joueurs y sont téléportés en attendant que la partie se remplisse, et elle
 * disparaît (les blocs d'origine sont restaurés) dès que la partie démarre.
 *
 * On ne construit plus la zone d'attente à la main via /bd <nom> lobby : elle est
 * entièrement automatique, centrée et dimensionnée d'après la config (lobby.cage-radius
 * / lobby.cage-height).
 */
public class WaitingLobbyManager {

    private final BedwarsPlugin plugin;
    /** Arènes pour lesquelles la cage est actuellement construite, avec les blocs d'origine à restaurer. */
    private final Map<String, Map<Location, BlockData>> activeCages = new HashMap<>();

    public WaitingLobbyManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    private int radius() {
        return Math.max(2, plugin.getConfig().getInt("lobby.cage-radius", 5));
    }

    private int height() {
        return Math.max(2, plugin.getConfig().getInt("lobby.cage-height", 4));
    }

    public boolean isBuilt(Arena arena) {
        return activeCages.containsKey(arena.getName().toLowerCase());
    }

    /** Centre du lobby d'attente (là où les joueurs sont téléportés), calé au centre du bloc. */
    public Location getCenter(Arena arena) {
        Location spec = arena.getSpecLocation();
        if (spec == null) return null;
        Location center = spec.clone();
        center.setX(spec.getBlockX() + 0.5);
        center.setZ(spec.getBlockZ() + 0.5);
        center.setY(spec.getBlockY());
        return center;
    }

    /** Construit la cage si elle n'existe pas déjà. */
    public void build(Arena arena) {
        String key = arena.getName().toLowerCase();
        if (activeCages.containsKey(key)) return;

        Location spec = arena.getSpecLocation();
        if (spec == null || spec.getWorld() == null) return;

        World world = spec.getWorld();
        int r = radius();
        int h = height();
        int baseX = spec.getBlockX();
        int baseY = spec.getBlockY();
        int baseZ = spec.getBlockZ();

        Map<Location, BlockData> original = new HashMap<>();
        BlockData barrier = Material.BARRIER.createBlockData();

        // Sol (juste sous les pieds), plafond, et 4 murs : une boîte fermée mais invisible.
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = -1; y <= h; y++) {
                    boolean isFloor = (y == -1);
                    boolean isCeiling = (y == h);
                    boolean isWall = (x == -r || x == r || z == -r || z == r);
                    if (!isFloor && !isCeiling && !isWall) continue; // intérieur laissé libre

                    Block block = world.getBlockAt(baseX + x, baseY + y, baseZ + z);
                    Location loc = block.getLocation();
                    original.put(loc, block.getBlockData());
                    block.setBlockData(barrier, false);
                }
            }
        }

        activeCages.put(key, original);
    }

    /** Détruit la cage (restaure les blocs d'origine) si elle existe. */
    public void destroy(Arena arena) {
        String key = arena.getName().toLowerCase();
        Map<Location, BlockData> original = activeCages.remove(key);
        if (original == null) return;
        for (Map.Entry<Location, BlockData> entry : original.entrySet()) {
            Location loc = entry.getKey();
            Block block = loc.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            block.setBlockData(entry.getValue(), false);
        }
    }

    /**
     * Vérifie qu'un emplacement est bien à l'intérieur de la cage de cette arène
     * (utilisé pour confiner les joueurs en attente / spectateurs "lobby").
     */
    public boolean isWithin(Arena arena, Location loc) {
        Location spec = arena.getSpecLocation();
        if (spec == null || loc == null || loc.getWorld() == null || !loc.getWorld().equals(spec.getWorld())) {
            return false;
        }
        int r = radius();
        int h = height();
        double dx = Math.abs(loc.getX() - (spec.getBlockX() + 0.5));
        double dz = Math.abs(loc.getZ() - (spec.getBlockZ() + 0.5));
        double dy = loc.getY() - spec.getBlockY();
        return dx <= r && dz <= r && dy >= -1 && dy <= h;
    }
}
