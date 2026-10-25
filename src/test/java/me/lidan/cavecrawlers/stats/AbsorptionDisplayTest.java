package me.lidan.cavecrawlers.stats;

import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbsorptionDisplayTest {
    private JavaPlugin plugin;
    private Player player;
    private YamlConfiguration config;
    private FakePackets packets;
    private AbsorptionDisplay display;
    private List<Runnable> nextTick;
    private Logger logger;

    @BeforeEach
    void setUp() {
        plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        player = mock(Player.class);
        config = new YamlConfiguration();
        nextTick = new ArrayList<>();
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getConfig()).thenReturn(config);
        logger = mock(Logger.class);
        when(plugin.getLogger()).thenReturn(logger);
        when(server.getScheduler()).thenReturn(scheduler);
        doReturn(List.of(player)).when(server).getOnlinePlayers();
        doAnswer(invocation -> {
            nextTick.add(invocation.getArgument(1));
            return null;
        }).when(scheduler).runTask(eq(plugin), any(Runnable.class));
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getEntityId()).thenReturn(42);
        when(player.isOnline()).thenReturn(true);
        when(player.getAbsorptionAmount()).thenReturn(3200D);
        packets = new FakePackets();
        display = new AbsorptionDisplay(plugin, packets);
    }

    @Test
    void initializesOnlinePlayersAndPreservesNativeAbsorptionAndHealth() {
        display.reload();
        assertEquals(List.of(20F), packets.sent);
        assertEquals(10, packets.update(player, 1600));
        verify(player, never()).setAbsorptionAmount(anyDouble());
        verify(player, never()).setHealth(anyDouble());
        verify(player, never()).setHealthScale(anyDouble());
    }

    @Test
    void playerPoolsAreIndependentAndReplacementEntitiesStartFresh() {
        display.reload();
        assertEquals(10, packets.update(player, 1600));
        assertEquals(20, packets.scaler.scale(UUID.randomUUID(), 43, 800));
        assertEquals(5, packets.update(player, 800));
        assertEquals(20, packets.scaler.scale(player.getUniqueId(), 100, 800));
    }

    @Test
    void disabledConfigurationRemovesInterceptionAndRestoresTheRealAmount() {
        display.reload();
        config.set("absorption-display.enabled", false);
        display.reload();
        assertEquals(List.of(20F, 3200F), packets.sent);
        assertFalse(packets.active);
        assertEquals(3200, packets.update(player, 3200));
    }

    @Test
    void disabledAtStartupInstallsNothingAndSendsNothing() {
        config.set("absorption-display.enabled", false);
        display.reload();
        assertFalse(packets.active);
        assertTrue(packets.sent.isEmpty());
    }

    @Test
    void closingTwiceRestoresNativeMetadataOnce() {
        display.reload();
        display.close();
        display.close();
        assertEquals(List.of(20F, 3200F), packets.sent);
        verify(player, never()).setAbsorptionAmount(anyDouble());
    }

    @Test
    void joinsRespawnsAndWorldChangesRefreshAfterTheirPacketsFinish() {
        display.reload();
        PlayerJoinEvent join = mock(PlayerJoinEvent.class);
        when(join.getPlayer()).thenReturn(player);
        PlayerRespawnEvent respawn = mock(PlayerRespawnEvent.class);
        when(respawn.getPlayer()).thenReturn(player);
        PlayerChangedWorldEvent world = mock(PlayerChangedWorldEvent.class);
        when(world.getPlayer()).thenReturn(player);

        display.onJoin(join);
        display.onRespawn(respawn);
        display.onWorldChange(world);
        assertEquals(1, packets.sent.size());
        assertEquals(3, nextTick.size());
        nextTick.forEach(Runnable::run);
        assertEquals(List.of(20F, 20F, 20F, 20F), packets.sent);
    }

    @Test
    void queuedRefreshDoesNotSendAfterShutdownOrDisconnect() {
        display.reload();
        PlayerRespawnEvent respawn = mock(PlayerRespawnEvent.class);
        when(respawn.getPlayer()).thenReturn(player);
        display.onRespawn(respawn);
        when(player.isOnline()).thenReturn(false);
        nextTick.forEach(Runnable::run);
        assertEquals(1, packets.sent.size());
        display.close();
        nextTick.forEach(Runnable::run);
        assertEquals(2, packets.sent.size());
    }

    @Test
    void deathAndQuitDiscardThePreviousPoolsPeak() {
        display.reload();
        PlayerDeathEvent death = mock(PlayerDeathEvent.class);
        when(death.getEntity()).thenReturn(player);
        display.onDeath(death);
        assertEquals(20, packets.update(player, 800));

        packets.update(player, 3200);
        PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
        when(quit.getPlayer()).thenReturn(player);
        display.onQuit(quit);
        assertEquals(20, packets.update(player, 400));
    }

    @Test
    void missingRuntimeMethodsDisableOnlyScalingAtStartup() {
        packets.startFailure = new NoSuchMethodError("Unsupported ProtocolLib runtime");
        display.reload();
        assertFalse(packets.active);
        assertTrue(packets.sent.isEmpty());
        verify(player, never()).setAbsorptionAmount(anyDouble());
        verify(logger).log(eq(Level.SEVERE), contains("Could not enable"), same(packets.startFailure));
    }

    @Test
    void missingProtocolLibClassesAreCaughtDuringLazyAdapterConstruction() {
        NoClassDefFoundError failure = new NoClassDefFoundError("Unsupported ProtocolLib class");
        display = new AbsorptionDisplay(plugin, () -> { throw failure; });
        display.reload();
        display.close();
        verify(logger).log(eq(Level.SEVERE), contains("may be unsupported"), same(failure));
        verify(player, never()).setAbsorptionAmount(anyDouble());
    }

    @Test
    void disabledDisplayDoesNotLinkProtocolLibClasses() {
        config.set("absorption-display.enabled", false);
        display = new AbsorptionDisplay(plugin, () -> { throw new AssertionError("Adapter must not be constructed"); });
        display.reload();
        display.close();
    }

    @Test
    void missingNativeAbsorptionApiDuringRefreshDisablesOnlyScaling() {
        display.reload();
        PlayerRespawnEvent respawn = mock(PlayerRespawnEvent.class);
        when(respawn.getPlayer()).thenReturn(player);
        display.onRespawn(respawn);
        NoSuchMethodError failure = new NoSuchMethodError("Changed native absorption API");
        doThrow(failure).when(player).getAbsorptionAmount();
        nextTick.removeFirst().run();
        assertEquals(3200, packets.update(player, 3200));
        doReturn(3200D).when(player).getAbsorptionAmount();
        nextTick.removeFirst().run();
        assertFalse(packets.active);
        assertEquals(List.of(20F, 3200F), packets.sent);
        verify(logger).log(eq(Level.WARNING), contains("compatibility error"), same(failure));
    }

    @Test
    void packetFailurePassesThroughImmediatelyAndRestoresNativeMetadataOnServerThread() {
        display.reload();
        assertEquals(10, packets.update(player, 1600));
        RuntimeException failure = new IllegalStateException("Changed packet layout");
        packets.onFailure.accept(failure);
        packets.onFailure.accept(failure);
        assertEquals(1600, packets.update(player, 1600));
        assertEquals(1, nextTick.size());
        nextTick.removeFirst().run();
        assertFalse(packets.active);
        assertEquals(List.of(20F, 3200F), packets.sent);
        verify(logger, times(1)).log(eq(Level.WARNING), contains("compatibility error"), same(failure));
        verify(player, never()).setAbsorptionAmount(anyDouble());
    }

    @Test
    void queuedFailureFromAnOldSessionCannotDisableANewReload() {
        display.reload();
        Consumer<Throwable> oldFailureHandler = packets.onFailure;
        oldFailureHandler.accept(new IllegalStateException("Old packet failure"));
        display.reload();
        nextTick.removeFirst().run();
        oldFailureHandler.accept(new IllegalStateException("Stale packet callback"));
        assertTrue(packets.active);
        assertEquals(10, packets.update(player, 1600));
    }

    @Test
    void delayedScalerFromAnOldSessionCannotChangeANewPoolsPeak() {
        display.reload();
        AbsorptionDisplay.Scaler oldScaler = packets.scaler;
        display.reload();
        assertEquals(6400, oldScaler.scale(player.getUniqueId(), player.getEntityId(), 6400));
        assertEquals(20, packets.update(player, 3200));
        assertEquals(10, packets.update(player, 1600));
    }

    @Test
    void outgoingRefreshFailureAlsoDisablesScaling() {
        display.reload();
        packets.sendFailure = new NoSuchMethodError("Changed native metadata method");
        PlayerRespawnEvent respawn = mock(PlayerRespawnEvent.class);
        when(respawn.getPlayer()).thenReturn(player);
        display.onRespawn(respawn);
        nextTick.removeFirst().run();
        assertEquals(3200, packets.update(player, 3200));
        packets.sendFailure = null;
        nextTick.removeFirst().run();
        assertFalse(packets.active);
        assertEquals(List.of(20F, 3200F), packets.sent);
    }

    @Test
    void cleanupFailureStillRestoresNativeMetadataAndDoesNotBlockShutdown() {
        display.reload();
        packets.stopFailure = new IllegalStateException("Listener already removed");
        display.close();
        assertFalse(packets.active);
        assertEquals(List.of(20F, 3200F), packets.sent);
        assertEquals(1600, packets.update(player, 1600));
    }

    private static final class FakePackets implements AbsorptionDisplay.Packets {
        private final List<Float> sent = new ArrayList<>();
        private AbsorptionDisplay.Scaler scaler;
        private Consumer<Throwable> onFailure;
        private boolean active;
        private Error startFailure;
        private Error sendFailure;
        private RuntimeException stopFailure;

        @Override
        public void start(AbsorptionDisplay.Scaler scaler, Consumer<Throwable> onFailure) {
            this.scaler = scaler;
            this.onFailure = onFailure;
            active = true;
            if (startFailure != null) {
                throw startFailure;
            }
        }

        @Override
        public void stop() {
            active = false;
            if (stopFailure != null) {
                throw stopFailure;
            }
        }

        @Override
        public void send(Player player, float amount) {
            if (sendFailure != null) {
                throw sendFailure;
            }
            sent.add(amount);
        }

        float update(Player player, float amount) {
            return scaler.scale(player.getUniqueId(), player.getEntityId(), amount);
        }
    }
}
