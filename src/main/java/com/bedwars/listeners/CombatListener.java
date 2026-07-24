package com.bedwars.listeners;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.ArenaTeam;
import com.bedwars.arena.TeamColor;
import com.bedwars.game.GameInstance;
import com.bedwars.util.BedUtil;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CombatListener implements Listener {

    private static final Set<Material> MINERAL_MATERIALS = Set.of(
            Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND, Material.EMERALD);

    private final BedwarsPlugin plugin;

    public CombatListener(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Pas de dégâts entre membres d'une même équipe. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player victim = event.getEntity() instanceof Player p ? p : null;
        if (victim == null) return;

        Player attacker = resolveAttacker(event);
        if (attacker == null || attacker.equals(victim)) return;

        GameInstance game = plugin.getGameManager().findInstanceOf(victim);
        if (game == null) return;
        if (game.isSameTeam(attacker, victim)) {
            event.setCancelled(true);
        }
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p) return p;
        if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
    }

    /** Destruction d'un lit (cassé comme un bloc normal). */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player breaker = event.getPlayer();
        GameInstance game = plugin.getGameManager().findInstanceOf(breaker);
        if (game == null) return;

        Arena arena = game.getArena();
        for (TeamColor color : TeamColor.forTeamCount(arena.getTeamCount())) {
            ArenaTeam team = arena.getTeams().get(color);
            if (team == null || team.getBedLocation() == null || team.isBedDestroyed()) continue;
            if (BedUtil.matchesBed(team.getBedLocation(), block)) {
                // On ne peut pas casser son propre lit.
                if (game.getTeamColor(breaker) == color) {
                    event.setCancelled(true);
                    breaker.sendMessage(ChatColor.RED + "Vous ne pouvez pas casser votre propre lit !");
                    return;
                }
                event.setCancelled(true); // on gère la casse nous-mêmes pour casser proprement les 2 blocs du lit
                for (Block b : BedUtil.getBedBlocks(team.getBedLocation().getWorld().getBlockAt(team.getBedLocation()))) {
                    b.setType(Material.AIR, false);
                }
                game.onBedBroken(color, breaker);
                return;
            }
        }

        // Blocs posés par les joueurs pendant la partie : cassables normalement.
        if (game.isPlacedBlock(block.getLocation())) {
            game.untrackBlock(block.getLocation());
            return;
        }

        // Sinon, c'est un bloc de la map d'origine : protégé.
        event.setCancelled(true);
    }

    /** Mort d'un joueur : loot limité aux minerais, messages, respawn ou élimination. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        GameInstance game = plugin.getGameManager().findInstanceOf(victim);
        if (game == null) return;

        Player killer = victim.getKiller();

        // Récupère uniquement les minerais de l'inventaire de la victime pour le tueur.
        List<ItemStack> minerals = new ArrayList<>();
        for (ItemStack drop : event.getDrops()) {
            if (drop != null && MINERAL_MATERIALS.contains(drop.getType())) {
                minerals.add(drop);
            }
        }
        event.getDrops().clear();
        event.setDroppedExp(0);

        if (killer != null) {
            for (ItemStack mineral : minerals) {
                killer.getInventory().addItem(mineral);
            }
        }

        TeamColor victimColor = game.getTeamColor(victim);
        TeamColor killerColor = killer != null ? game.getTeamColor(killer) : null;
        ArenaTeam victimTeam = victimColor != null ? game.getArena().getTeams().get(victimColor) : null;
        boolean finalKill = victimTeam != null && victimTeam.isBedDestroyed();

        // Message de mort personnalisé et coloré par équipe.
        String victimName = victimColor != null ? victimColor.getChatColor() + victim.getName() : victim.getName();
        String message;
        if (killer != null) {
            String killerName = killerColor != null ? killerColor.getChatColor() + killer.getName() : killer.getName();
            message = victimName + ChatColor.GRAY + " a été tué par " + killerName + ChatColor.GRAY
                    + (finalKill ? " (Final Kill)" : "");
        } else {
            message = victimName + ChatColor.GRAY + " est mort.";
        }
        event.setDeathMessage(null);
        for (Player p : game.getAllParticipants()) {
            p.sendMessage(message);
        }

        if (killer != null) {
            if (finalKill) {
                game.addFinalKill(killer.getUniqueId());
            } else {
                game.addKill(killer.getUniqueId());
            }
        }

        if (finalKill) {
            game.onPlayerFinalDeath(victim);
        }
        // Sinon, le respawn normal est géré par PlayerRespawnEvent.
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GameInstance game = plugin.getGameManager().findInstanceOf(player);
        if (game == null) return;

        TeamColor color = game.getTeamColor(player);
        ArenaTeam team = color != null ? game.getArena().getTeams().get(color) : null;

        if (team != null && !team.isBedDestroyed() && team.getSpawnLocation() != null) {
            event.setRespawnLocation(team.getSpawnLocation());
            plugin.getServer().getScheduler().runTask(plugin, () -> game.respawnAtTeamSpawn(player));
        } else if (game.getArena().getSpecLocation() != null) {
            event.setRespawnLocation(game.getArena().getSpecLocation());
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.setGameMode(GameMode.SPECTATOR);
                game.toSpectator(player);
            });
        }
    }
}
