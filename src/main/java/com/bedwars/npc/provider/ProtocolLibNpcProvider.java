package com.bedwars.npc.provider;

import com.bedwars.npc.NpcRecord;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Implémentation "faux joueur" (skin Minecraft classique) via ProtocolLib.
 *
 * ⚠️ C'est la partie la plus bas-niveau / fragile de tout le plugin : elle envoie
 * directement des paquets réseau Minecraft (PLAYER_INFO, SPAWN_ENTITY, ENTITY_LOOK...),
 * dont le format évolue à chaque version majeure du jeu. Le code ci-dessous vise
 * Minecraft 1.21 / ProtocolLib récent ; si l'affichage ne fonctionne pas exactement
 * comme prévu chez toi, c'est très probablement ici qu'il faut ajuster les choses
 * (vérifier la version de ProtocolLib installée, comparer avec sa documentation).
 */
public class ProtocolLibNpcProvider implements NpcProvider {

    public interface ClickHandler {
        void onClick(Player player, UUID npcId);
    }

    private final Plugin plugin;
    private final Logger logger;
    private final ProtocolManager protocolManager;
    private final ClickHandler clickHandler;

    private final Map<Integer, UUID> entityIdToNpc = new ConcurrentHashMap<>();
    private final Map<UUID, WrappedGameProfile> profiles = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(2_000_000_000);

    public ProtocolLibNpcProvider(Plugin plugin, ClickHandler clickHandler) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.clickHandler = clickHandler;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        registerClickListener();
    }

    @Override
    public void spawn(NpcRecord record) {
        int entityId = idCounter.incrementAndGet();
        UUID fakeUuid = UUID.randomUUID();
        record.setEntityId(entityId);
        record.setEntityUuid(fakeUuid);
        entityIdToNpc.put(entityId, record.getId());

        String base = record.getType().name().equals("SHOP") ? "Marchand" : "Amelioration";
        String name = (base + "_" + (entityId % 10000));
        WrappedGameProfile profile = new WrappedGameProfile(fakeUuid, name.length() > 16 ? name.substring(0, 16) : name);
        profiles.put(record.getId(), profile);

        for (Player online : Bukkit.getOnlinePlayers()) {
            showTo(record, online);
        }
    }

    @Override
    public void showTo(NpcRecord record, Player viewer) {
        WrappedGameProfile profile = profiles.get(record.getId());
        if (profile == null) return;
        try {
            PacketContainer addInfo = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
            addInfo.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            List<PlayerInfoData> data = new ArrayList<>();
            data.add(new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL,
                    WrappedChatComponent.fromText(profile.getName())));
            addInfo.getPlayerInfoDataLists().write(0, data);
            protocolManager.sendServerPacket(viewer, addInfo);

            PacketContainer spawn = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            spawn.getIntegers().write(0, record.getEntityId());
            spawn.getUUIDs().write(0, record.getEntityUuid());
            spawn.getEntityTypeModifier().write(0, EntityType.PLAYER);
            Location loc = record.getLocation();
            spawn.getDoubles().write(0, loc.getX());
            spawn.getDoubles().write(1, loc.getY());
            spawn.getDoubles().write(2, loc.getZ());
            spawn.getBytes().write(0, (byte) (loc.getYaw() * 256 / 360));
            spawn.getBytes().write(1, (byte) (loc.getPitch() * 256 / 360));
            protocolManager.sendServerPacket(viewer, spawn);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    PacketContainer removeInfo = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
                    removeInfo.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
                    removeInfo.getPlayerInfoDataLists().write(0, data);
                    protocolManager.sendServerPacket(viewer, removeInfo);
                } catch (Exception ignored) {
                    // Purement cosmétique (tab-list) : on ignore une éventuelle incompatibilité de version.
                }
            }, 40L);
        } catch (Exception e) {
            logger.warning("[NPC ProtocolLib] Échec de l'affichage du NPC : " + e.getMessage()
                    + " — vérifie la compatibilité de version ProtocolLib/serveur.");
        }
    }

    @Override
    public void despawn(NpcRecord record) {
        entityIdToNpc.remove(record.getEntityId());
        profiles.remove(record.getId());
        try {
            PacketContainer destroy = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            destroy.getIntLists().write(0, Collections.singletonList(record.getEntityId()));
            for (Player online : Bukkit.getOnlinePlayers()) {
                protocolManager.sendServerPacket(online, destroy);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void lookAt(NpcRecord record, Player target) {
        Location loc = record.getLocation();
        double dx = target.getLocation().getX() - loc.getX();
        double dz = target.getLocation().getZ() - loc.getZ();
        double dy = target.getEyeLocation().getY() - (loc.getY() + 1.6);
        double distXZ = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, distXZ));

        try {
            PacketContainer headRot = protocolManager.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
            headRot.getIntegers().write(0, record.getEntityId());
            headRot.getBytes().write(0, (byte) (yaw * 256 / 360));

            PacketContainer look = protocolManager.createPacket(PacketType.Play.Server.ENTITY_LOOK);
            look.getIntegers().write(0, record.getEntityId());
            look.getBytes().write(0, (byte) (yaw * 256 / 360));
            look.getBytes().write(1, (byte) (pitch * 256 / 360));
            look.getBooleans().write(0, true);

            for (Player online : Bukkit.getOnlinePlayers()) {
                protocolManager.sendServerPacket(online, headRot);
                protocolManager.sendServerPacket(online, look);
            }
        } catch (Exception ignored) {
            // Purement cosmétique (rotation de la tête) : on ignore une éventuelle incompatibilité de version.
        }
    }

    @Override
    public boolean handlesInteractionItself() {
        return true;
    }

    private void registerClickListener() {
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                int targetId = event.getPacket().getIntegers().read(0);
                UUID npcId = entityIdToNpc.get(targetId);
                if (npcId == null) return;
                Bukkit.getScheduler().runTask(plugin, () -> clickHandler.onClick(event.getPlayer(), npcId));
            }
        });
    }
}
