package com.bedwars.npc.provider;

import com.bedwars.npc.NpcRecord;
import com.bedwars.shop.ShopType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import java.util.UUID;

public class VillagerNpcProvider implements NpcProvider {

    @Override
    public void spawn(NpcRecord record) {
        Location loc = record.getLocation();
        Villager villager = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setSilent(true);
        villager.setCollidable(false);
        villager.setPersistent(true);
        villager.setProfession(record.getType() == ShopType.SHOP
                ? Villager.Profession.WEAPONSMITH : Villager.Profession.LIBRARIAN);
        villager.setCustomName(ChatColor.GOLD + (record.getType() == ShopType.SHOP ? "Marchand" : "Amélioration"));
        villager.setCustomNameVisible(true);
        record.setEntityUuid(villager.getUniqueId());
    }

    @Override
    public void showTo(NpcRecord record, Player viewer) {
        // Le villageois est une vraie entité du monde, visible nativement par tous : rien à faire.
    }

    @Override
    public void despawn(NpcRecord record) {
        UUID id = record.getEntityUuid();
        if (id == null) return;
        org.bukkit.entity.Entity e = Bukkit.getEntity(id);
        if (e != null) e.remove();
    }

    @Override
    public void lookAt(NpcRecord record, Player target) {
        UUID id = record.getEntityUuid();
        if (id == null) return;
        org.bukkit.entity.Entity e = Bukkit.getEntity(id);
        if (e == null) return;
        Location loc = e.getLocation();
        double dx = target.getLocation().getX() - loc.getX();
        double dz = target.getLocation().getZ() - loc.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        loc.setYaw(yaw);
        e.teleport(loc);
    }

    @Override
    public boolean handlesInteractionItself() {
        return false;
    }
}
