package com.bedwars.listeners;

import com.bedwars.BedwarsPlugin;
import com.bedwars.game.GameInstance;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Quand un item de fer/or tombé au sol (cas de secours, si personne n'était sur le
 * générateur au moment du spawn) est ramassé alors que plusieurs joueurs sont présents
 * en même temps, on répartit équitablement au lieu de tout donner au premier arrivé.
 * Le diamant et l'émeraude ne sont volontairement jamais concernés par ce partage.
 */
public class OreMergeListener implements Listener {

    private static final Set<Material> SPLITTABLE = Set.of(Material.IRON_INGOT, Material.GOLD_INGOT);
    private static final double RADIUS = 3.0;

    private final BedwarsPlugin plugin;

    public OreMergeListener(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player picker)) return;
        Item item = event.getItem();
        if (!SPLITTABLE.contains(item.getItemStack().getType())) return;

        GameInstance game = plugin.getGameManager().findInstanceOf(picker);
        if (game == null) return;

        List<Player> nearby = new ArrayList<>();
        for (Player p : game.getAllParticipants()) {
            if (game.isAlivePlaying(p) && p.getWorld().equals(item.getWorld())
                    && p.getLocation().distance(item.getLocation()) <= RADIUS) {
                nearby.add(p);
            }
        }
        if (nearby.size() <= 1) return; // rien à répartir, comportement normal

        event.setCancelled(true);
        ItemStack stack = item.getItemStack();
        int amount = stack.getAmount();
        item.remove();

        int index = 0;
        while (amount > 0) {
            Player target = nearby.get(index % nearby.size());
            ItemStack single = stack.clone();
            single.setAmount(1);
            target.getInventory().addItem(single);
            amount--;
            index++;
        }
    }
}
