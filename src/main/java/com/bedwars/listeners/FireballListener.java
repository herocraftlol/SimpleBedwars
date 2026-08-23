package com.bedwars.listeners;

import com.bedwars.BedwarsPlugin;
import com.bedwars.game.GameInstance;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * La boule de feu (Fire Charge) achetée au shop fonctionne comme sur Hypixel Bedwars : clic droit
 * en main l'envoie tout droit dans la direction regardée (au lieu du comportement vanilla, qui se
 * contente d'allumer un feu sur le bloc visé), explose au premier obstacle touché — ne cassant que
 * les blocs posés par les joueurs (déjà géré génériquement par TntListener#onExplode, qui
 * s'applique à toute explosion, boule de feu comprise) — et propulse en l'air quiconque se trouve
 * tout près de l'impact (notamment soi-même, si on la tire sous ses pieds).
 */
public class FireballListener implements Listener {

    private final BedwarsPlugin plugin;

    public FireballListener(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return; // évite le double-déclenchement main/off-hand
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.FIRE_CHARGE) return;

        Player player = event.getPlayer();
        GameInstance game = plugin.getGameManager().findInstanceOf(player);
        if (game == null) return;

        event.setCancelled(true); // empêche le comportement vanilla (allumer un feu sur le bloc visé)

        Vector direction = player.getEyeLocation().getDirection().normalize();
        Fireball fireball = player.launchProjectile(Fireball.class, direction.multiply(1.6));
        fireball.setShooter(player);
        fireball.setDirection(direction);
        fireball.setYield(1.3f);       // explosion modérée, façon Hypixel (pas un gros cratère)
        fireball.setIsIncendiary(false); // ne met pas le feu aux alentours

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Fireball)) return;

        // Propulsion : quiconque se trouve tout près de l'impact (notamment sous ses propres
        // pieds si on tire vers le sol) est envoyé en l'air, comme sur Hypixel.
        Location impact = event.getLocation();
        for (Entity nearby : impact.getWorld().getNearbyEntities(impact, 3.5, 3.5, 3.5)) {
            if (!(nearby instanceof Player p)) continue;
            double distance = p.getLocation().distance(impact);
            if (distance > 3.5) continue;

            Vector push = p.getLocation().toVector().subtract(impact.toVector());
            if (push.lengthSquared() < 0.0001) push = new Vector(0, 0, 0);
            else push.normalize();

            double falloff = Math.max(0.2, 1 - (distance / 3.5));
            push.multiply(1.1 * falloff);
            push.setY(Math.max(push.getY(), 0.5) + 0.6 * falloff); // bonne impulsion verticale
            p.setVelocity(push);
        }
    }
}
