package me.lidan.cavecrawlers.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.MinecraftKey;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.storage.PlayerSkillsManager;
import me.lidan.cavecrawlers.storage.db.Database;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PacketManager {

    private static final Logger log = LoggerFactory.getLogger(PacketManager.class);
    private static final String EXPERIMENTAL_SAFE_LOAD_KEY = "experimental.safe-load";
    private static PacketManager instance;

    public static PacketManager getInstance() {
        if (instance == null) {
            instance = new PacketManager();
        }
        return instance;
    }

    public void cancelDamageIndicatorParticle() {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(CaveCrawlers.getInstance(), ListenerPriority.NORMAL, PacketType.Play.Server.WORLD_PARTICLES) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        PacketContainer packet = event.getPacket();
                        Particle particle = packet.getNewParticles().readSafely(0).getParticle();
                        if (particle == Particle.DAMAGE_INDICATOR) {
                            event.setCancelled(true);
                        }
                    }

                }
        );
    }

    public void preventClientPacketsDuringPlayerDataLoad() {
        if (!CaveCrawlers.getInstance().getConfig().getBoolean(EXPERIMENTAL_SAFE_LOAD_KEY, true)) {
            return;
        }

        Database database = Database.getInstance();
        PlayerSkillsManager skillsManager = PlayerSkillsManager.getInstance();
        PacketType[] guardedPackets = getSupportedGuardedClientPackets();
        if (guardedPackets.length == 0) {
            log.warn("No supported movement/block-dig client packet types found; load guard listener not registered.");
            return;
        }

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                CaveCrawlers.getInstance(),
                ListenerPriority.NORMAL,
                guardedPackets
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (!database.isAvailable() || skillsManager.isLoaded(event.getPlayer().getUniqueId())) {
                    return;
                }

                event.setCancelled(true);
            }
        });
    }

    private PacketType[] getSupportedGuardedClientPackets() {
        List<PacketType> candidates = List.of(
                // movement
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK,
                PacketType.Play.Client.LOOK,
                PacketType.Play.Client.GROUND,
                PacketType.Play.Client.VEHICLE_MOVE,
                PacketType.Play.Client.STEER_VEHICLE,
                PacketType.Play.Client.BOAT_MOVE,
                // interactions/combat
                PacketType.Play.Client.ARM_ANIMATION,
                PacketType.Play.Client.USE_ENTITY,
                PacketType.Play.Client.USE_ITEM,
                PacketType.Play.Client.BLOCK_PLACE,
                PacketType.Play.Client.BLOCK_DIG,
                PacketType.Play.Client.ENTITY_ACTION,
                PacketType.Play.Client.HELD_ITEM_SLOT
        );

        List<PacketType> supported = new ArrayList<>();
        for (PacketType packetType : candidates) {
            if (packetType.isSupported()) {
                supported.add(packetType);
            }
        }
        return supported.toArray(PacketType[]::new);
    }

    public void setBlockDestroyStage(Player player, Location location, int stage) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        PacketContainer blockBreakAnimation = protocolManager.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);

        int fakeId = location.hashCode();

        // Set the necessary fields
        blockBreakAnimation.getIntegers()
                .write(0, fakeId) // Entity ID, you might need to replace this with a valid entity ID
                .write(1, stage); // Destroy stage

        blockBreakAnimation.getBlockPositionModifier()
                .write(0, new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ())); // Block position

        // Send the packet to the player
        player.getWorld().getPlayers().forEach(loopPlayer -> {
            protocolManager.sendServerPacket(loopPlayer, blockBreakAnimation);
        });
    }

    public void setCooldown(Player player, Material material, int cooldown) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SET_COOLDOWN);

        packet.getIntegers().write(0, cooldown);

        MinecraftKey key = new MinecraftKey(material.getKey().getNamespace(), material.getKey().getKey());
        packet.getMinecraftKeys().write(0, key);

        try {
            protocolManager.sendServerPacket(player, packet);
        } catch (Exception e) {
            log.warn("Failed to send SetCooldown packet to player {}: {}", player.getName(), e.getMessage());
        }
    }
}
