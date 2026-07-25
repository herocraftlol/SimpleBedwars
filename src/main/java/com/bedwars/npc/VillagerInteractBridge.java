package com.bedwars.npc;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class VillagerInteractBridge implements Listener {

    private final HumanNPCManager manager;

    public VillagerInteractBridge(HumanNPCManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        NpcRecord record = manager.getByEntityUuid(event.getRightClicked().getUniqueId());
        if (record == null) return;
        event.setCancelled(true);
        manager.handleClickFromBridge(event.getPlayer(), record);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (manager.getByEntityUuid(event.getEntity().getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }
}
