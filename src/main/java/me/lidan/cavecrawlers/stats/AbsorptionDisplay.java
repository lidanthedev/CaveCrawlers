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
import java.util.logging.Level;

/** Owns display state only. Native absorption, expiry, and damage remain owned by Minecraft. */
public final class AbsorptionDisplay implements Listener {
    private final JavaPlugin plugin;
    private final Packets packets;
    private final ConcurrentMap<UUID, TrackedPlayer> players = new ConcurrentHashMap<>();
    private volatile boolean enabled;

    public AbsorptionDisplay(JavaPlugin plugin) {
        this(plugin, new AbsorptionPackets(plugin));
    }

    AbsorptionDisplay(JavaPlugin plugin, Packets packets) {
        this.plugin = plugin;
        this.packets = packets;
    }

    /** Called on the server thread at startup and after gameplay configuration reloads. */
    public void reload() {
        close();
        if (!plugin.getConfig().getBoolean("absorption-display.enabled", true)) {
            return;
        }
        try {
            packets.start(this::scale);
            enabled = true;
            plugin.getServer().getOnlinePlayers().forEach(this::refresh);
        } catch (RuntimeException exception) {
            close();
            plugin.getLogger().log(Level.SEVERE, "Could not enable absorption display scaling; native absorption is unchanged", exception);
        }
    }

    /** Remove interception before restoring the client's real native absorption value. */
    public void close() {
        boolean wasEnabled = enabled;
        enabled = false;
        packets.stop();
        if (wasEnabled) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                send(player, (float) player.getAbsorptionAmount());
            }
        }
        players.clear();
    }

    private float scale(UUID playerId, int entityId, float absorption) {
        if (!enabled) {
            return absorption;
        }
        TrackedPlayer tracked = players.compute(playerId, (id, previous) ->
                previous == null || previous.entityId() != entityId
                        ? new TrackedPlayer(entityId, new AbsorptionScale()) : previous);
        return tracked.scale().update(absorption);
    }

    private void refresh(Player player) {
        if (enabled && player.isOnline()) {
            send(player, scale(player.getUniqueId(), player.getEntityId(), (float) player.getAbsorptionAmount()));
        }
    }

    private void send(Player player, float absorption) {
        try {
            packets.send(player, absorption);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not refresh absorption display for " + player.getUniqueId(), exception);
        }
    }

    private void resetAndRefresh(Player player) {
        players.remove(player.getUniqueId());
        // Respawn/world packets must finish before the replacement entity's metadata is sent.
        plugin.getServer().getScheduler().runTask(plugin, () -> refresh(player));
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
        players.remove(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        players.remove(event.getPlayer().getUniqueId());
    }

    private record TrackedPlayer(int entityId, AbsorptionScale scale) {
    }

    interface Packets {
        void start(Scaler scaler);

        void stop();

        void send(Player player, float absorption);
    }

    @FunctionalInterface
    interface Scaler {
        float scale(UUID playerId, int entityId, float absorption);
    }
}
