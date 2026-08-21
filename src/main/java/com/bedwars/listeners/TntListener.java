package com.bedwars.listeners;

import com.bedwars.BedwarsPlugin;
import com.bedwars.game.GameInstance;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Iterator;

/**
 * La TNT achetée au shop s'amorce toute seule dès qu'elle est posée (comme sur Hypixel Bedwars,
 * pas besoin de silex et acier), et ne peut casser que les blocs posés par les joueurs pendant la
 * partie — jamais la structure d'origine de la map.
 */
public class TntListener implements Listener {

    private final BedwarsPlugin plugin;

    public TntListener(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.TNT) return;
        GameInstance game = plugin.getGameManager().findInstanceOf(event.getPlayer());
        if (game == null) return;

        event.setCancelled(true);
        Block block = event.getBlock();
        Location loc = block.getLocation().add(0.5, 0, 0.5);
        block.setType(Material.AIR, false);

        TNTPrimed tnt = block.getWorld().spawn(loc, TNTPrimed.class);
        tnt.setFuseTicks(80); // 4 secondes, comme la TNT vanilla amorcée au silex et acier
        Player source = event.getPlayer();
        tnt.setSource(source);
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        GameInstance game = findGameNear(event.getLocation());
        if (game == null) return;

        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            Block b = it.next();
            if (game.isPlacedBlock(b.getLocation())) {
                game.untrackBlock(b.getLocation());
            } else {
                // Bloc d'origine de la map : jamais cassable par une explosion.
                it.remove();
            }
        }
    }

    private GameInstance findGameNear(Location loc) {
        for (var arena : plugin.getArenaManager().getArenas().values()) {
            var instance = plugin.getGameManager().getExistingInstance(arena);
            if (instance == null) continue;
            var pos1 = arena.getGamePos1();
            var pos2 = arena.getGamePos2();
            if (pos1 == null || pos2 == null || !pos1.getWorld().equals(loc.getWorld())) continue;
            double minX = Math.min(pos1.getX(), pos2.getX()) - 1, maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
            double minY = Math.min(pos1.getY(), pos2.getY()) - 1, maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
            double minZ = Math.min(pos1.getZ(), pos2.getZ()) - 1, maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;
            if (loc.getX() >= minX && loc.getX() <= maxX && loc.getY() >= minY && loc.getY() <= maxY
                    && loc.getZ() >= minZ && loc.getZ() <= maxZ) {
                return instance;
            }
        }
        return null;
    }
}
