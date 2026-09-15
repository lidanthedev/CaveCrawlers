package me.lidan.cavecrawlers.storage;

import me.lidan.cavecrawlers.skills.*;
import me.lidan.cavecrawlers.storage.db.*;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.io.File;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real manager + H2, with a controllable Bukkit task queue and monotonic clock. */
class PlayerSkillsManagerLifecycleTest {
    private final Queue<Runnable> async = new ConcurrentLinkedQueue<>();
    private final Queue<Runnable> delayedAsync = new ConcurrentLinkedQueue<>();
    private final Queue<Runnable> delayedMain = new ConcurrentLinkedQueue<>();
    private final Queue<Runnable> main = new ConcurrentLinkedQueue<>();
    private ExecutorService executor;
    private final AtomicLong clock = new AtomicLong();
    private PlayerSkillsManager manager;
    private Database database;
    private Jdbi jdbi;
    private UUID uuid;
    private Player player;
    private Server server;
    private JavaPlugin plugin;
    private SkillsManager skillsManager;
    private SkillInfo mining;
    private MockedStatic<JavaPlugin> javaPlugin;

    @BeforeEach
    void setUp() throws Exception {
        executor = Executors.newCachedThreadPool();
        plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(new File("build/test-plugin"));
        YamlConfiguration config = new YamlConfiguration();
        config.set("database.shutdown-flush-timeout", 1);
        when(plugin.getConfig()).thenReturn(config);
        javaPlugin = mockStatic(JavaPlugin.class);
        javaPlugin.when(() -> JavaPlugin.getProvidingPlugin(any())).thenReturn(plugin);
        skillsManager = new SkillsManager();
        mining = mock(SkillInfo.class);
        when(mining.getId()).thenReturn("mining");
        when(mining.getName()).thenReturn("mining");
        when(mining.getMaxLevel()).thenReturn(10);
        when(mining.getXpToLevelList()).thenReturn(List.of(100D, 100D));
        skillsManager.getSkillInfoMap().put("mining", mining);

        server = mock(Server.class);
        Thread primary = Thread.currentThread();
        when(server.isPrimaryThread()).thenAnswer(i -> Thread.currentThread() == primary);
        when(server.getLogger()).thenReturn(Logger.getLogger("lifecycle-test"));
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(i -> { main.add(i.getArgument(1)); return mock(BukkitTask.class); });
        when(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable.class))).thenAnswer(i -> { async.add(i.getArgument(1)); return mock(BukkitTask.class); });
        when(scheduler.runTaskLaterAsynchronously(eq(plugin), any(Runnable.class), anyLong())).thenAnswer(i -> {
            delayedAsync.add(i.getArgument(1)); return mock(BukkitTask.class);
        });
        when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), anyLong())).thenAnswer(i -> {
            delayedMain.add(i.getArgument(1)); return mock(BukkitTask.class);
        });
        setStatic(Bukkit.class, "server", server);
        uuid = UUID.randomUUID();
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(server.getPlayer(uuid)).thenReturn(player);
        doReturn(List.of(player)).when(server).getOnlinePlayers();
        jdbi = Jdbi.create("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbi.installPlugin(new SqlObjectPlugin());
        var constructor = Database.class.getDeclaredConstructor(Jdbi.class, boolean.class);
        constructor.setAccessible(true);
        database = spy(constructor.newInstance(jdbi, false));
        database.registerTable(new SkillsTable());
        database.registerTable(new PlayerSessionsTable());
        manager = new PlayerSkillsManager(plugin, database, clock::get,
                LeaseConfig.validated(60, 10, ignored -> fail("Valid config warned")));
        setStatic(PlayerSkillsManager.class, "instance", manager);
        manager.setServerId();
        manager.heartbeat();
        manager.loadPlayerAsync(uuid);
        runAsync();
        runMain();
        assertTrue(manager.canPersistPlayer(uuid));
    }

    @AfterEach
    void cleanUp() throws Exception {
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        javaPlugin.close();
        setStatic(PlayerSkillsManager.class, "instance", null);
        setStatic(Bukkit.class, "server", null);
    }

    @Test
    void briefOutageAndRecoveryPreserveSession() {
        Skills original = manager.getSkills(uuid);
        clock.set(Duration.ofSeconds(15).toNanos());
        doThrow(new IllegalStateException("network down")).when(database).heartbeatAll(anyString());
        manager.heartbeat();
        manager.checkLeaseHealth();
        assertTrue(manager.canPersistPlayer(uuid));
        skillsManager.giveXp(player, mining, 5, false);
        assertEquals(5, original.get(mining).getTotalXp());
        clock.set(Duration.ofSeconds(30).toNanos());
        doCallRealMethod().when(database).heartbeatAll(anyString());
        manager.heartbeat();
        clock.set(Duration.ofSeconds(50).toNanos());
        manager.checkLeaseHealth();
        assertTrue(manager.canPersistPlayer(uuid));
        assertSame(original, manager.getSkills(uuid));
        verify(player, never()).kick(any(net.kyori.adventure.text.Component.class));
    }

    @Test
    void prolongedOutageRejectsXpAndAddonMutationBeforeWatchdogRuns() throws Exception {
        Skills old = manager.getSkills(uuid);
        skillsManager.giveXp(player, mining, 5, false);
        clock.set(Duration.ofSeconds(40).toNanos());
        assertFalse(manager.isLoaded(uuid));
        skillsManager.giveXp(player, mining, 5, false);
        assertEquals(5, old.get(mining).getTotalXp());
        assertThrows(IllegalStateException.class, () -> old.addXp(mining, 3));
        assertThrows(IllegalStateException.class, () -> old.get(mining).setTotalXp(99));
        manager.putSkills(uuid, new Skills());
        manager.resetPlayerData(uuid);
        assertEquals(5, old.get(mining).getTotalXp());
        doThrow(new IllegalStateException("network down")).when(database).persistPlayer(any(), anyString(), anyLong(), anyLong(), anyList(), anyBoolean(), anyBoolean());
        manager.checkLeaseHealth();
        manager.checkLeaseHealth();
        verify(player, times(1)).kick(any(net.kyori.adventure.text.Component.class));
        runAsync(); // Failed final snapshot remains queued with its original fence.
        doCallRealMethod().when(database).persistPlayer(any(), anyString(), anyLong(), anyLong(), anyList(), anyBoolean(), anyBoolean());
        manager.heartbeat();
        runAsync();
        runMain();
        assertFalse(manager.canPersistPlayer(uuid));
        assertEquals(5, persistedXp());
        assertThrows(IllegalStateException.class, () -> old.addXp(mining, 1));
    }

    @Test
    void experimentalLevelMutatorsAlsoRejectUnsafeSession() throws Exception {
        var levels = mock(me.lidan.cavecrawlers.levels.LevelConfigManager.class, CALLS_REAL_METHODS);
        var config = mock(me.lidan.cavecrawlers.utils.CustomConfig.class);
        Field field = levels.getClass().getDeclaredField("config");
        field.setAccessible(true);
        field.set(levels, config);
        levels.setPlayerXP(uuid.toString(), 10);
        verify(config).set("players." + uuid + ".xp", 10);
        clearInvocations(config);
        clock.set(Duration.ofSeconds(40).toNanos());
        levels.setPlayerXP(uuid.toString(), 99);
        levels.setPlayerLevel(uuid.toString(), 99);
        levels.givePlayerXP(player, 99);
        verifyNoInteractions(config);
    }

    @Test
    void persistentWriteFailureStopsProgressEvenWhenHeartbeatsSucceed() throws Exception {
        doThrow(new IllegalStateException("addon table write failed")).when(database)
                .persistPlayer(any(), anyString(), anyLong(), anyLong(), anyList(), anyBoolean(), anyBoolean());
        manager.savePlayerNow(uuid);
        runAsync();
        clock.set(Duration.ofSeconds(30).toNanos());
        manager.heartbeat();
        assertTrue(manager.canPersistPlayer(uuid));
        clock.set(Duration.ofSeconds(41).toNanos());
        assertFalse(manager.canPersistPlayer(uuid));
        manager.checkLeaseHealth();
        verify(player).kick(any(net.kyori.adventure.text.Component.class));
    }

    @Test
    void lateHeartbeatResponseCannotReviveOldSessions() {
        doAnswer(invocation -> {
            clock.set(Duration.ofSeconds(61).toNanos());
            return invocation.callRealMethod();
        }).when(database).heartbeatAll(anyString());
        manager.heartbeat();
        assertFalse(manager.canPersistPlayer(uuid));
        manager.checkLeaseHealth();
        verify(player).kick(any(net.kyori.adventure.text.Component.class));
    }

    @Test
    void recoveryAfterDeadlineRevokesEvenWithoutInterveningWatchdog() {
        clock.set(Duration.ofSeconds(41).toNanos());
        manager.heartbeat();
        assertFalse(manager.canPersistPlayer(uuid));
        manager.checkLeaseHealth();
        verify(player).kick(any(net.kyori.adventure.text.Component.class));
    }

    @Test
    void staleWriteInvalidatesPlayer() throws Exception {
        jdbi.useHandle(h -> h.execute("UPDATE player_sessions SET lock_timestamp=0"));
        database.acquirePlayerSession(uuid, "new-backend", 60_000).orElseThrow();
        manager.savePlayerNow(uuid);
        runAsync();
        runMain();
        assertFalse(manager.canPersistPlayer(uuid));
        verify(player).kick(any(net.kyori.adventure.text.Component.class));
    }

    @Test
    void delayedStaleKickCannotInvalidateReconnectedGeneration() throws Exception {
        jdbi.useHandle(h -> h.execute("UPDATE player_sessions SET lock_timestamp=0"));
        var other = database.acquirePlayerSession(uuid, "other", 60_000).orElseThrow();
        manager.savePlayerNow(uuid);
        runAsync(); // Rejection schedules a kick, but the main thread has not handled it yet.
        database.releasePlayerSession(uuid, "other", other.fenceToken());
        manager.loadPlayerAsync(uuid);
        runAsync();
        runMain();
        assertTrue(manager.canPersistPlayer(uuid));
        verify(player, never()).kick(any(net.kyori.adventure.text.Component.class));
    }

    @Test
    void rapidReconnectWaitsForQuitAndRejectsOldReferences() throws Exception {
        Skills old = manager.getSkills(uuid);
        old.addXp(mining, 25);
        manager.savePlayerNowOnQuit(uuid);
        manager.loadPlayerAsync(uuid);
        assertFalse(manager.canPersistPlayer(uuid));
        runAsync();
        runMain();
        runAsync();
        runMain();
        assertTrue(manager.canPersistPlayer(uuid));
        assertEquals(25, manager.getSkills(uuid).get(mining).getTotalXp());
        assertThrows(IllegalStateException.class, () -> old.addXp(mining, 5));
        assertThrows(IllegalStateException.class, () -> manager.putSkills(uuid, old));
        verify(player, never()).kick(any(net.kyori.adventure.text.Component.class));
    }

    @Test
    void repeatedCachePutsDoNotRebindLiveMutationGuards() throws Exception {
        Skills skills = manager.getSkills(uuid);
        for (int i = 0; i < 2000; i++) manager.putSkills(uuid, skills);
        skills.addXp(mining, 3);
        manager.savePlayerNow(uuid);
        runAsync();
        assertEquals(3, persistedXp());
    }

    @Test
    void explicitReloadHandsOffExistingStateBeforeLoading() throws Exception {
        manager.getSkills(uuid).addXp(mining, 23);
        manager.loadPlayerAsync(uuid);
        runAsync(); runMain(); runAsync(); runMain();
        assertTrue(manager.canPersistPlayer(uuid));
        assertEquals(23, manager.getSkills(uuid).get(mining).getTotalXp());
    }

    @Test
    void loadPublicationAfterQuitDoesNotRepopulateCache() throws Exception {
        when(server.getPlayer(uuid)).thenReturn(null);
        manager.savePlayerNowOnQuit(uuid);
        runAsync(); runMain();
        when(server.getPlayer(uuid)).thenReturn(player);
        manager.loadPlayerAsync(uuid);
        runAsync(); // SQL completed, publication still queued.
        manager.savePlayerNowOnQuit(uuid);
        when(server.getPlayer(uuid)).thenReturn(null);
        runMain(); runAsync();
        assertFalse(manager.isLoaded(uuid));
        assertEquals(0, jdbi.withHandle(h -> h.createQuery("SELECT is_locked FROM player_sessions")
                .mapTo(Integer.class).one()).intValue());
    }

    @Test
    void supersededLoadPublicationRetriesForOnlinePlayer() throws Exception {
        when(server.getPlayer(uuid)).thenReturn(null);
        manager.savePlayerNowOnQuit(uuid);
        runAsync(); runMain();
        when(server.getPlayer(uuid)).thenReturn(player);
        manager.loadPlayerAsync(uuid);
        runAsync(); // SQL completed, publication still queued.

        manager.loadPlayerAsync(uuid);
        assertEquals(1, async.size()); // Ownership release only; the old scheduled marker blocks another load.
        runMain();  // Rejects the old publication after clearing its scheduled marker.
        runAsync(); // Releases the old fence; the coordinated retry waits for that release.
        runDelayedMain();
        runAsync(); runMain();

        assertTrue(manager.canPersistPlayer(uuid));
        verify(player, never()).kick(any(net.kyori.adventure.text.Component.class));
    }

    @Test
    void duplicateLoadCannotPublishOverProgression() throws Exception {
        when(server.getPlayer(uuid)).thenReturn(null);
        manager.savePlayerNowOnQuit(uuid);
        runAsync(); runMain();
        when(server.getPlayer(uuid)).thenReturn(player);
        manager.loadPlayerAsync(uuid);
        runAsync();
        manager.scheduleLoadIfNeeded(uuid); // Publication pending: must not queue another DB load.
        assertTrue(async.isEmpty());
        runMain();
        manager.getSkills(uuid).addXp(mining, 17);
        runAsync(); runMain();
        assertEquals(17, manager.getSkills(uuid).get(mining).getTotalXp());
    }

    @Test
    void writerCoalescesNewerSnapshotsWhileOldWriteIsRunning() throws Exception {
        CountDownLatch writing = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        doAnswer(invocation -> {
            writing.countDown();
            assertTrue(finish.await(5, TimeUnit.SECONDS));
            return invocation.callRealMethod();
        }).when(database).persistPlayer(eq(uuid), anyString(), anyLong(), eq(1L), anyList(), anyBoolean(), anyBoolean());
        manager.getSkills(uuid).addXp(mining, 10);
        manager.savePlayerNow(uuid);
        Future<?> writer = executor.submit(async.remove());
        try {
            assertTrue(writing.await(5, TimeUnit.SECONDS));
            for (int i = 0; i < 3; i++) {
                manager.getSkills(uuid).addXp(mining, 1);
                manager.savePlayerNow(uuid);
            }
            finish.countDown();
            writer.get(5, TimeUnit.SECONDS);
        } finally { finish.countDown(); }
        assertEquals(13, persistedXp());
        verify(database, times(2)).persistPlayer(eq(uuid), anyString(), anyLong(), anyLong(), anyList(), anyBoolean(), anyBoolean());
    }

    @Test
    void resetAndQuitBarriersSurviveActualWriterCoalescing() throws Exception {
        jdbi.useHandle(h -> h.attach(SkillsDao.class).upsertSkills(List.of(
                new SkillRow(uuid.toString(), "removed-skill", 800, 0, 800))));
        manager.getSkills(uuid).addXp(mining, 80);
        manager.savePlayerNow(uuid);
        manager.resetPlayerData(uuid);
        manager.getSkills(uuid).addXp(mining, 12);
        manager.savePlayerNowOnQuit(uuid);
        runAsync();
        assertEquals(12, persistedXp());
        assertEquals(1, jdbi.withHandle(h -> h.createQuery("SELECT COUNT(*) FROM skills").mapTo(Integer.class).one()).intValue());
        verify(database, times(1)).persistPlayer(eq(uuid), anyString(), anyLong(), anyLong(), anyList(), eq(true), eq(true));
        assertEquals(0, jdbi.withHandle(h -> h.createQuery("SELECT is_locked FROM player_sessions").mapTo(Integer.class).one()).intValue());
    }

    @Test
    void migrationBarrierCoversFlushAndCopy() throws Exception {
        exerciseMigrationBarrier(false);
    }

    @Test
    void failedMigrationUnfreezesSource() throws Exception {
        exerciseMigrationBarrier(true);
    }

    private void exerciseMigrationBarrier(boolean failCopy) throws Exception {
        manager.getSkills(uuid).addXp(mining, 13);
        CountDownLatch copying = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        var migration = executor.submit(() -> manager.withMigrationBarrier(() -> {
            assertEquals(13, persistedXp());
            copying.countDown();
            try { assertTrue(finish.await(5, TimeUnit.SECONDS)); }
            catch (InterruptedException e) { throw new RuntimeException(e); }
            if (failCopy) throw new IllegalStateException("copy failed");
        }));
        try {
            long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
            while (main.isEmpty() && System.nanoTime() < deadline) Thread.sleep(1);
            assertFalse(main.isEmpty());
            runMain(); runAsync();
            assertTrue(copying.await(5, TimeUnit.SECONDS));
            assertFalse(manager.canPersistPlayer(uuid));
            assertThrows(IllegalStateException.class, () -> manager.getSkills(uuid).addXp(mining, 5));
            manager.savePlayerNow(uuid);
            manager.resetPlayerData(uuid);
            assertTrue(async.isEmpty());
            assertEquals(13, persistedXp());
        } finally { finish.countDown(); }
        if (failCopy) assertThrows(ExecutionException.class, () -> migration.get(5, TimeUnit.SECONDS));
        else migration.get(5, TimeUnit.SECONDS);
        assertTrue(manager.canPersistPlayer(uuid));
    }

    @Test
    void failedLoadQuitRetriesReleaseBeforeReconnect() throws Exception {
        when(server.getPlayer(uuid)).thenReturn(null);
        manager.savePlayerNowOnQuit(uuid);
        runAsync(); runMain();
        when(server.getPlayer(uuid)).thenReturn(player);
        doThrow(new IllegalStateException("load failed")).when(database).loadPlayer(any(), anyString(), anyLong());
        manager.loadPlayerAsync(uuid);
        runAsync();
        doThrow(new IllegalStateException("release failed")).when(database).releasePlayerSession(any(), anyString(), anyLong());
        manager.savePlayerNowOnQuit(uuid);
        runAsync();
        assertFalse(delayedAsync.isEmpty());
        doCallRealMethod().when(database).loadPlayer(any(), anyString(), anyLong());
        doCallRealMethod().when(database).releasePlayerSession(any(), anyString(), anyLong());
        executor.submit(delayedAsync.remove()).get(5, TimeUnit.SECONDS);
        manager.loadPlayerAsync(uuid);
        runAsync(); runMain();
        assertTrue(manager.canPersistPlayer(uuid));
    }

    @Test
    void shutdownFlushesEvenIfScheduledWriterNeverStarted() {
        manager.getSkills(uuid).addXp(mining, 22);
        manager.savePlayerNow(uuid);
        async.clear(); // Bukkit cancels queued tasks before onDisable.
        manager.shutdown();
        assertEquals(22, persistedXp());
        assertFalse(manager.canPersistPlayer(uuid));
        var order = inOrder(database);
        order.verify(database).persistPlayer(eq(uuid), anyString(), anyLong(), anyLong(), anyList(), anyBoolean(), eq(true));
        order.verify(database).releaseAllLocks(anyString());
    }

    @Test
    void shutdownDoesNotReleaseWhileWriterStillRuns() throws Exception {
        CountDownLatch writing = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        doAnswer(invocation -> {
            writing.countDown(); assertTrue(finish.await(5, TimeUnit.SECONDS));
            return invocation.callRealMethod();
        }).when(database).persistPlayer(any(), anyString(), anyLong(), anyLong(), anyList(), anyBoolean(), anyBoolean());
        manager.savePlayerNow(uuid);
        var writer = executor.submit(async.remove());
        try {
            assertTrue(writing.await(5, TimeUnit.SECONDS));
            manager.shutdown();
            verify(database, never()).releaseAllLocks(anyString());
        } finally { finish.countDown(); }
        writer.get(5, TimeUnit.SECONDS);
        assertFalse(manager.canPersistPlayer(uuid));
    }

    @Test
    void shutdownFailureRetainsSnapshotAndDoesNotBulkRelease() {
        manager.getSkills(uuid).addXp(mining, 7);
        doThrow(new IllegalStateException("network down")).when(database).persistPlayer(any(), anyString(), anyLong(), anyLong(), anyList(), anyBoolean(), anyBoolean());
        manager.shutdown();
        verify(database, never()).releaseAllLocks(anyString());
    }

    private void runAsync() throws Exception {
        for (Runnable task; (task = async.poll()) != null;) executor.submit(task).get(5, TimeUnit.SECONDS);
    }

    private void runMain() {
        for (Runnable task; (task = main.poll()) != null;) task.run();
    }

    private void runDelayedMain() {
        int queued = delayedMain.size();
        for (int i = 0; i < queued; i++) delayedMain.remove().run();
    }

    private double persistedXp() {
        return jdbi.withHandle(h -> h.createQuery("SELECT total_xp FROM skills WHERE player_uuid=:uuid")
                .bind("uuid", uuid.toString()).mapTo(Double.class).findOne().orElse(0D));
    }

    private static void setStatic(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
