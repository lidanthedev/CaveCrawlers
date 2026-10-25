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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** ProtocolLib boundary for the owning player's absorption metadata. */
final class AbsorptionPackets implements AbsorptionDisplay.Packets {
    private final JavaPlugin plugin;
    private ProtocolManager protocol;
    private PacketAdapter listener;
    private WrappedDataWatcherObject absorption;
    private AtomicBoolean stopped = new AtomicBoolean(true);

    AbsorptionPackets(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start(AbsorptionDisplay.Scaler scaler, Consumer<Throwable> onFailure) {
        protocol = ProtocolLibrary.getProtocolManager();
        absorption = resolveAbsorptionAccessor();
        validatePacketFormat();
        AtomicBoolean inactive = new AtomicBoolean(false);
        stopped = inactive;
        listener = new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.ENTITY_METADATA) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (inactive.get() || event.isCancelled()) {
                    return;
                }
                PacketContainer original = event.getPacket();
                try {
                    Player recipient = event.getPlayer();
                    int entityId = original.getIntegers().read(0);
                    if (entityId != recipient.getEntityId()) {
                        return;
                    }
                    List<WrappedDataValue> values = original.getDataValueCollectionModifier().read(0);
                    if (values == null) {
                        throw new IllegalStateException("Player metadata list is missing");
                    }
                    for (int i = 0; i < values.size(); i++) {
                        WrappedDataValue value = values.get(i);
                        if (value.getIndex() != absorption.getIndex()) {
                            continue;
                        }
                        if (!(value.getValue() instanceof Float real)) {
                            throw new IllegalStateException("Player absorption metadata is no longer a float");
                        }
                        float displayed = scaler.scale(recipient.getUniqueId(), entityId, real);
                        // Metadata packets and their lists may be shared with other viewers.
                        List<WrappedDataValue> replacement = new ArrayList<>(values);
                        replacement.set(i, new WrappedDataValue(value.getIndex(), value.getSerializer(), displayed));
                        PacketContainer copy = original.shallowClone();
                        copy.getDataValueCollectionModifier().write(0, replacement);
                        event.setPacket(copy);
                        return;
                    }
                } catch (RuntimeException | LinkageError failure) {
                    // The original packet has never been mutated. Further packets pass through.
                    if (inactive.compareAndSet(false, true)) {
                        onFailure.accept(failure);
                    }
                }
            }
        };
        protocol.addPacketListener(listener);
    }

    @Override
    public void stop() {
        stopped.set(true);
        if (listener != null) {
            PacketAdapter previous = listener;
            listener = null;
            protocol.removePacketListener(previous);
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

    static WrappedDataWatcherObject resolveAbsorptionAccessor() {
        Object handle = AbsorptionAccessorResolver.resolve(
                MinecraftReflection.getEntityHumanClass(),
                MinecraftReflection.getMinecraftClass("world.entity.LivingEntity", "world.entity.EntityLiving", "EntityLiving"),
                MinecraftReflection.getDataWatcherObjectClass(), value -> {
                    var serializer = new WrappedDataWatcherObject(value).getSerializer();
                    return serializer != null && (serializer.getType() == Float.class || serializer.getType() == float.class);
                });
        WrappedDataWatcherObject accessor = new WrappedDataWatcherObject(handle);
        if (accessor.getIndex() < 0 || accessor.getIndex() >= 255) {
            throw new IllegalStateException("Invalid player absorption metadata index");
        }
        return accessor;
    }

    private void validatePacketFormat() {
        PacketContainer packet = protocol.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, 0);
        packet.getDataValueCollectionModifier().write(0, List.of(
                new WrappedDataValue(absorption.getIndex(), absorption.getSerializer(), 1.25F)));
        List<WrappedDataValue> values = packet.getDataValueCollectionModifier().read(0);
        if (values == null || values.size() != 1 || values.getFirst().getIndex() != absorption.getIndex()
                || !Float.valueOf(1.25F).equals(values.getFirst().getValue())) {
            throw new IllegalStateException("Unsupported player metadata packet format");
        }
        PacketContainer copy = packet.shallowClone();
        copy.getDataValueCollectionModifier().write(0, List.of(
                new WrappedDataValue(absorption.getIndex(), absorption.getSerializer(), 2.5F)));
        if (!Float.valueOf(1.25F).equals(packet.getDataValueCollectionModifier().read(0).getFirst().getValue())) {
            throw new IllegalStateException("Player metadata packets cannot be copied independently");
        }
    }
}
