package me.lidan.cavecrawlers.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedParticle;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.utils.RandomUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

public class PacketManager {

    private static PacketManager instance;

    public void cancelDamageIndicatorParticle(){
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

    public void setBlockDestroyStage(Player player, Location location, int stage){
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

    public static PacketManager getInstance() {
        if (instance == null){
            instance = new PacketManager();
        }
        return instance;
    }
}
