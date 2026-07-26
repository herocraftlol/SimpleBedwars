package com.bedwars.npc;

import com.bedwars.shop.ShopType;
import org.bukkit.Location;

import java.util.UUID;

public class NpcRecord {

    private final UUID id;
    private final ShopType type;
    private final Location location;
    /** Entité réellement présente dans le monde : soit un faux joueur (paquet), soit un villageois (repli). */
    private UUID entityUuid;
    private int entityId; // id d'entité côté paquet (NPC à skin de joueur uniquement)

    public NpcRecord(UUID id, ShopType type, Location location) {
        this.id = id;
        this.type = type;
        this.location = location;
    }

    public UUID getId() {
        return id;
    }

    public ShopType getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public UUID getEntityUuid() {
        return entityUuid;
    }

    public void setEntityUuid(UUID entityUuid) {
        this.entityUuid = entityUuid;
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }
}
