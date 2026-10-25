package me.lidan.cavecrawlers.stats;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

/** Owns display state only. Native absorption, expiry, and damage remain owned by Minecraft. */
public final class AbsorptionDisplay implements Listener {
    private final JavaPlugin plugin;
    private final Supplier<Packets> packetFactory;
    private Packets packets;
    private volatile Session session = new Session();
    private boolean restoreRequired;

    public AbsorptionDisplay(JavaPlugin plugin) {
        this(plugin, () -> new AbsorptionPackets(plugin));
    }

    AbsorptionDisplay(JavaPlugin plugin, Packets packets) {
        this(plugin, () -> packets);
    }

    AbsorptionDisplay(JavaPlugin plugin, Supplier<Packets> packetFactory) {
        this.plugin = plugin;
        this.packetFactory = packetFactory;
    }

    /** Called on the server thread at startup and after gameplay configuration reloads. */
    public void reload() {
        close();
        if (!plugin.getConfig().getBoolean("absorption-display.enabled", true)) {
            return;
        }
        Session starting = session;
        try {
            // Resolve/link ProtocolLib classes inside the compatibility guard, not the constructor.
            if (packets == null) {
                packets = packetFactory.get();
            }
            packets.start((id, entity, amount) -> scale(starting, id, entity, amount),
                    failure -> fail(starting, failure));
            starting.enabled.set(true);
            restoreRequired = true;
            plugin.getServer().getOnlinePlayers().forEach(this::refresh);
        } catch (RuntimeException | LinkageError exception) {
            close();
            plugin.getLogger().log(Level.SEVERE, "Could not enable absorption display scaling; the server or ProtocolLib version may be unsupported. Native absorption is unchanged", exception);
        }
    }

    /** Remove interception before restoring the client's real native absorption value. */
    public void close() {
        Session closing = session;
        closing.enabled.set(false);
        session = new Session();
        Throwable cleanupFailure = null;
        try {
            if (packets != null) {
                packets.stop();
            }
        } catch (RuntimeException | LinkageError failure) {
            cleanupFailure = failure;
        }
        if (restoreRequired) {
            restoreRequired = false;
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                try {
                    packets.send(player, (float) player.getAbsorptionAmount());
                } catch (RuntimeException | LinkageError failure) {
                    if (cleanupFailure == null) {
                        cleanupFailure = failure;
                    }
                }
            }
        }
        closing.players.clear();
        if (cleanupFailure != null) {
            plugin.getLogger().log(Level.WARNING, "Absorption scaling is disabled, but client restoration failed; subsequent native metadata updates will pass through", cleanupFailure);
        }
    }

    private void fail(Session failedSession, Throwable failure) {
        if (failedSession != session || !failedSession.enabled.compareAndSet(true, false)) {
            return;
        }
        plugin.getLogger().log(Level.WARNING, "Absorption display scaling disabled after a compatibility error; native absorption is unchanged", failure);
        Runnable cleanup = () -> {
            if (failedSession == session) {
                close();
            }
        };
        if (plugin.getServer().isPrimaryThread()) {
            cleanup.run();
        } else {
            try {
                plugin.getServer().getScheduler().runTask(plugin, cleanup);
            } catch (RuntimeException | LinkageError schedulingFailure) {
                // Interception already passes through; shutdown can finish the cleanup.
                plugin.getLogger().log(Level.WARNING, "Could not schedule absorption display cleanup", schedulingFailure);
            }
        }
    }

    private float scale(Session scalingSession, UUID playerId, int entityId, float absorption) {
        if (scalingSession != session || !scalingSession.enabled.get()) {
            return absorption;
        }
        TrackedPlayer tracked = scalingSession.players.compute(playerId, (id, previous) ->
                previous == null || previous.entityId() != entityId
                        ? new TrackedPlayer(entityId, new AbsorptionScale()) : previous);
        float scaled = tracked.scale().update(absorption);
        return scalingSession == session && scalingSession.enabled.get() ? scaled : absorption;
    }

    private void refresh(Player player) {
        Session refreshing = session;
        try {
            if (refreshing.enabled.get() && player.isOnline()) {
                packets.send(player, scale(refreshing, player.getUniqueId(), player.getEntityId(), (float) player.getAbsorptionAmount()));
            }
        } catch (RuntimeException | LinkageError exception) {
            fail(refreshing, exception);
        }
    }

    private void resetAndRefresh(Player player) {
        Session resetting = session;
        if (!resetting.enabled.get()) {
            return;
        }
        try {
            resetting.players.remove(player.getUniqueId());
            // Respawn/world packets must finish before the replacement entity's metadata is sent.
            plugin.getServer().getScheduler().runTask(plugin, () -> refresh(player));
        } catch (RuntimeException | LinkageError exception) {
            fail(resetting, exception);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        resetAndRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        resetAndRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        resetAndRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        session.players.remove(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        session.players.remove(event.getPlayer().getUniqueId());
    }

    private record TrackedPlayer(int entityId, AbsorptionScale scale) {
    }

    private static final class Session {
        private final AtomicBoolean enabled = new AtomicBoolean();
        private final ConcurrentMap<UUID, TrackedPlayer> players = new ConcurrentHashMap<>();
    }

    interface Packets {
        void start(Scaler scaler, Consumer<Throwable> onFailure);

        void stop();

        void send(Player player, float absorption);
    }

    @FunctionalInterface
    interface Scaler {
        float scale(UUID playerId, int entityId, float absorption);
    }
}
