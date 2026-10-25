package me.lidan.cavecrawlers.stats;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.WrappedDataWatcherObject;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/** ProtocolLib boundary for the owning player's absorption metadata. */
final class AbsorptionPackets implements AbsorptionDisplay.Packets {
    private final JavaPlugin plugin;
    private ProtocolManager protocol;
    private PacketAdapter listener;
    private WrappedDataWatcherObject absorption;

    AbsorptionPackets(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start(AbsorptionDisplay.Scaler scaler) {
        protocol = ProtocolLibrary.getProtocolManager();
        absorption = resolveAbsorptionAccessor();
        listener = new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.ENTITY_METADATA) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.isCancelled()) {
                    return;
                }
                Player recipient = event.getPlayer();
                PacketContainer original = event.getPacket();
                int entityId = original.getIntegers().read(0);
                if (entityId != recipient.getEntityId()) {
                    return;
                }
                List<WrappedDataValue> values = original.getDataValueCollectionModifier().read(0);
                if (values == null) {
                    return;
                }
                for (int i = 0; i < values.size(); i++) {
                    WrappedDataValue value = values.get(i);
                    if (value.getIndex() == absorption.getIndex() && value.getValue() instanceof Float real) {
                        float displayed = scaler.scale(recipient.getUniqueId(), entityId, real);
                        // Metadata packets and their lists may be shared with other viewers.
                        List<WrappedDataValue> replacement = new ArrayList<>(values);
                        replacement.set(i, new WrappedDataValue(value.getIndex(), value.getSerializer(), displayed));
                        PacketContainer copy = original.shallowClone();
                        copy.getDataValueCollectionModifier().write(0, replacement);
                        event.setPacket(copy);
                        return;
                    }
                }
            }
        };
        protocol.addPacketListener(listener);
    }

    @Override
    public void stop() {
        if (listener != null) {
            protocol.removePacketListener(listener);
            listener = null;
        }
    }

    @Override
    public void send(Player player, float amount) {
        PacketContainer packet = protocol.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, player.getEntityId());
        packet.getDataValueCollectionModifier().write(0, List.of(
                new WrappedDataValue(absorption.getIndex(), absorption.getSerializer(), amount)));
        // The amount is already scaled, or deliberately raw during shutdown.
        protocol.sendServerPacket(player, packet, false);
    }

    private static WrappedDataWatcherObject resolveAbsorptionAccessor() {
        for (Class<?> type = MinecraftReflection.getEntityHumanClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField("DATA_PLAYER_ABSORPTION_ID");
                field.setAccessible(true);
                return new WrappedDataWatcherObject(field.get(null));
            } catch (NoSuchFieldException ignored) {
                // The accessor can move to a superclass between Minecraft versions.
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Cannot read the native player absorption accessor", exception);
            }
        }
        throw new IllegalStateException("Native player absorption accessor not found; Paper 1.21.11+ is required");
    }
}
