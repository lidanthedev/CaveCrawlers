package me.lidan.cavecrawlers.storage.db;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class DatabasePersistenceTest {
    protected Jdbi jdbi;
    protected Database otherDatabase;
    protected Database database;
    protected UUID uuid;

    @BeforeEach
    void setUp() {
        jdbi = Jdbi.create("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbi.installPlugin(new SqlObjectPlugin());
        database = new Database(jdbi, false);
        database.registerTable(new SkillsTable());
        database.registerTable(new PlayerSessionsTable());
        otherDatabase = new Database(jdbi, false);
        uuid = UUID.randomUUID();
    }

    @Test
    void takeoverFencesOldSaveAndUnlock() {
        Database.PlayerLease a = acquire("A");
        assertTrue(database.persistPlayer(uuid, "A", a.fenceToken(), 1, rows(1_000), false, false).committed());

        expireLease();
        Database.PlayerLease b = acquire("B");
        assertTrue(b.fenceToken() > a.fenceToken());
        assertEquals(0, database.releasePlayerSession(uuid, "A", a.fenceToken()));
        assertFalse(database.persistPlayer(uuid, "A", a.fenceToken(), 2, rows(2_000), false, false).committed());
        assertTrue(otherDatabase.persistPlayer(uuid, "B", b.fenceToken(), 2, rows(3_000), false, false).committed());

        assertEquals(3_000, totalXp());
        assertEquals("B", lockingServer());
    }

    @Test
    void heartbeatUsesDatabaseTimeWithoutChangingFence() {
        Database.PlayerLease lease = acquire("A");
        expireLease();
        assertEquals(1, database.heartbeatAll("A"));
        long timestamp = jdbi.withHandle(handle -> handle.createQuery(
                        "SELECT lock_timestamp FROM player_sessions WHERE player_uuid = :uuid")
                .bind("uuid", uuid.toString()).mapTo(Long.class).one());
        assertTrue(timestamp > 0);
        assertEquals(lease.fenceToken(), database.acquirePlayerSession(uuid, "A", 60_000).orElseThrow().fenceToken());
    }

    @Test
    void sameLiveOwnerRefreshesLeaseWithoutAdvancingFence() {
        Database.PlayerLease first = acquire("A");
        Database.PlayerLease refreshed = database.acquirePlayerSession(uuid, "A", 60_000).orElseThrow();

        assertEquals(first.fenceToken(), refreshed.fenceToken());
        assertEquals(first.dataRevision(), refreshed.dataRevision());
        assertEquals("A", lockingServer());
    }

    @Test
    void fenceOverflowRollsBackSessionAcquisition() {
        Database.PlayerLease initial = acquire("A");
        database.releasePlayerSession(uuid, "A", initial.fenceToken());
        jdbi.useHandle(handle -> handle.createUpdate(
                        "UPDATE player_sessions SET fence_token = :fence, data_revision = 7, " +
                                "is_locked = 0, locking_server = NULL, lock_timestamp = 0 WHERE player_uuid = :uuid")
                .bind("fence", Long.MAX_VALUE)
                .bind("uuid", uuid.toString())
                .execute());

        assertThrows(ArithmeticException.class,
                () -> database.acquirePlayerSession(uuid, "A", 60_000));
        assertEquals(Long.MAX_VALUE, fenceToken());
        assertEquals(0, isLocked());
        assertEquals(7, dataRevision());
    }

    @Test
    void resetOverflowRollsBackWithoutDeletingSkills() {
        Database.PlayerLease initial = acquire("A");
        database.releasePlayerSession(uuid, "A", initial.fenceToken());
        jdbi.useHandle(handle -> handle.attach(SkillsDao.class).upsertSkills(rows(42)));
        jdbi.useHandle(handle -> handle.createUpdate(
                        "UPDATE player_sessions SET fence_token = :fence, data_revision = :revision, " +
                                "is_locked = 0, locking_server = NULL, lock_timestamp = 0 WHERE player_uuid = :uuid")
                .bind("fence", Long.MAX_VALUE)
                .bind("revision", Long.MAX_VALUE)
                .bind("uuid", uuid.toString())
                .execute());

        assertThrows(ArithmeticException.class, () -> database.resetOfflinePlayer(uuid, 60_000));
        assertEquals(42, totalXp());
        assertEquals(Long.MAX_VALUE, fenceToken());
        assertEquals(Long.MAX_VALUE, dataRevision());
    }

    @Test
    void registeredAddonTablesSaveInRegistrationOrder() {
        List<String> order = new CopyOnWriteArrayList<>();
        database.registerTable(new OrderedAddonTable("first", order));
        database.registerTable(new OrderedAddonTable("second", order));
        Database.PlayerLease lease = acquire("A");

        assertTrue(database.persistPlayer(uuid, "A", lease.fenceToken(), 1, rows(3), false, false).committed());
        assertEquals(List.of("first", "second"), order);
    }

    @Test
    void newerRevisionRejectsOutOfOrderSave() {
        Database.PlayerLease lease = acquire("A");
        assertTrue(database.persistPlayer(uuid, "A", lease.fenceToken(), 11, rows(1_500), false, false).committed());
        assertFalse(database.persistPlayer(uuid, "A", lease.fenceToken(), 10, rows(1_000), false, false).committed());
        assertEquals(1_500, totalXp());
    }

    @Test
    void quitCommitIsVisibleBeforeTransfer() {
        Database.PlayerLease a = acquire("A");
        assertTrue(database.persistPlayer(uuid, "A", a.fenceToken(), 1, rows(5_000), false, true).committed());

        Database.PlayerLease b = acquire("B");
        assertTrue(b.fenceToken() > a.fenceToken());
        assertEquals(5_000, otherDatabase.loadPlayer(uuid, "B", b.fenceToken()).getFirst().getTotalXp());
    }

    @Test
    void rapidReconnectOnSameBackendGetsNewFence() {
        Database.PlayerLease first = acquire("A");
        assertTrue(database.persistPlayer(uuid, "A", first.fenceToken(), 1, rows(500), false, true).committed());

        Database.PlayerLease reconnected = acquire("A");
        assertTrue(reconnected.fenceToken() > first.fenceToken());
        assertTrue(database.persistPlayer(uuid, "A", reconnected.fenceToken(), 2, rows(750), false, false).committed());
        assertFalse(database.persistPlayer(uuid, "A", first.fenceToken(), 2, rows(600), false, true).committed());
        assertEquals(750, totalXp());
    }

    @Test
    void rapidTransferBackRejectsBothPreviousBackends() {
        Database.PlayerLease a1 = acquire("A");
        assertTrue(database.persistPlayer(uuid, "A", a1.fenceToken(), 1, rows(100), false, true).committed());
        Database.PlayerLease b = acquire("B");
        assertTrue(otherDatabase.persistPlayer(uuid, "B", b.fenceToken(), 2, rows(200), false, true).committed());
        Database.PlayerLease a2 = acquire("A");
        assertTrue(database.persistPlayer(uuid, "A", a2.fenceToken(), 3, rows(300), false, false).committed());

        assertFalse(database.persistPlayer(uuid, "A", a1.fenceToken(), 4, rows(150), false, false).committed());
        assertFalse(otherDatabase.persistPlayer(uuid, "B", b.fenceToken(), 4, rows(250), false, false).committed());
        assertEquals(300, totalXp());
    }

    @Test
    void failedQuitRetryCannotOverwriteNewOwner() {
        ToggleAddonTable addon = new ToggleAddonTable();
        database.registerTable(addon);
        Database.PlayerLease a = acquire("A");
        assertTrue(database.persistPlayer(uuid, "A", a.fenceToken(), 1, rows(4_500), false, false).committed());

        addon.fail.set(true);
        assertThrows(RuntimeException.class,
                () -> database.persistPlayer(uuid, "A", a.fenceToken(), 2, rows(5_000), false, true));
        assertEquals(4_500, totalXp());

        addon.fail.set(false);
        expireLease();
        Database.PlayerLease b = acquire("B");
        assertTrue(otherDatabase.persistPlayer(uuid, "B", b.fenceToken(), 2, rows(6_000), false, false).committed());
        assertFalse(database.persistPlayer(uuid, "A", a.fenceToken(), 2, rows(5_000), false, true).committed());
        assertEquals(6_000, totalXp());
    }

    @Test
    void addonFailureRollsBackSkillsAndRevision() {
        ToggleAddonTable addon = new ToggleAddonTable();
        database.registerTable(addon);
        Database.PlayerLease lease = acquire("A");
        assertTrue(database.persistPlayer(uuid, "A", lease.fenceToken(), 1, rows(100), false, false).committed());

        addon.fail.set(true);
        assertThrows(RuntimeException.class,
                () -> database.persistPlayer(uuid, "A", lease.fenceToken(), 2, rows(200), false, true));
        assertEquals(100, totalXp());
        assertEquals(1, dataRevision());
        assertEquals(1, addonPayload());
        assertEquals("A", lockingServer());
    }

    @Test
    void resetIsOrderedWithSaves() {
        Database.PlayerLease lease = acquire("A");
        assertTrue(database.persistPlayer(uuid, "A", lease.fenceToken(), 30, rows(1_000), false, false).committed());
        assertTrue(database.persistPlayer(uuid, "A", lease.fenceToken(), 31, List.of(), true, false).committed());
        assertTrue(database.persistPlayer(uuid, "A", lease.fenceToken(), 32, rows(250), false, false).committed());
        assertFalse(database.persistPlayer(uuid, "A", lease.fenceToken(), 31, List.of(), true, false).committed());
        assertEquals(250, totalXp());
    }

    @Test
    void coalescedPostResetSnapshotRejectsBothOlderResetAndOlderSave() {
        var lease = acquire("A");
        database.persistPlayer(uuid, "A", lease.fenceToken(), 29,
                List.of(new SkillRow(uuid.toString(), "removed-skill", 900, 0, 900)), false, false);
        // The writer carries the reset barrier onto the latest snapshot.
        assertTrue(database.persistPlayer(uuid, "A", lease.fenceToken(), 32, rows(250), true, false).committed());
        assertFalse(database.persistPlayer(uuid, "A", lease.fenceToken(), 31, List.of(), true, false).committed());
        assertFalse(database.persistPlayer(uuid, "A", lease.fenceToken(), 30, rows(1000), false, false).committed());
        assertEquals(250, totalXp());
        assertEquals(1, database.loadPlayer(uuid, "A", lease.fenceToken()).size());
    }

    @Test
    void offlineResetRefusesLiveOwnerAndFencesExpiredOwner() {
        Database.PlayerLease old = acquire("A");
        assertFalse(database.resetOfflinePlayer(uuid, 60_000));
        expireLease();
        assertTrue(database.resetOfflinePlayer(uuid, 60_000));
        Database.PlayerLease next = acquire("B");
        assertTrue(next.fenceToken() > old.fenceToken());
    }

    @Test
    void offlineResetClearsRegisteredAddonRowsWithSkills() {
        ToggleAddonTable addon = new ToggleAddonTable();
        database.registerTable(addon);
        Database.PlayerLease lease = acquire("A");
        assertTrue(database.persistPlayer(uuid, "A", lease.fenceToken(), 1, rows(11), false, true).committed());

        assertTrue(database.resetOfflinePlayer(uuid, 60_000));

        assertEquals(0, jdbi.withHandle(handle -> handle.createQuery(
                "SELECT COUNT(*) FROM addon_test WHERE player_uuid=:uuid")
                .bind("uuid", uuid.toString()).mapTo(Integer.class).one()).intValue());
    }

    @Test
    void legacyMigrationNeverOverwritesDatabase() {
        jdbi.useHandle(handle -> handle.attach(SkillsDao.class).upsertSkills(rows(5_000)));
        assertFalse(database.importLegacySkills(uuid, rows(1_000)));
        assertEquals(5_000, totalXp());
        assertFalse(database.importLegacySkills(uuid, rows(9_000)));
        assertEquals(5_000, totalXp());
    }

    @Test
    void legacyMigrationImportsMissingPlayerOnce() {
        assertTrue(database.importLegacySkills(uuid, rows(1_000)));
        assertEquals(1_000, totalXp());
        assertFalse(database.importLegacySkills(uuid, rows(2_000)));
        assertEquals(1_000, totalXp());
    }

    @Test
    void playerSessionVersionOneUpgradesInPlace() {
        Jdbi oldJdbi = Jdbi.create("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        oldJdbi.installPlugin(new SqlObjectPlugin());
        Database oldDatabase = new Database(oldJdbi, false);
        oldJdbi.useHandle(handle -> {
            handle.execute("CREATE TABLE player_sessions (player_uuid VARCHAR(36) PRIMARY KEY, " +
                    "is_locked TINYINT NOT NULL DEFAULT 0, locking_server VARCHAR(64), " +
                    "lock_timestamp BIGINT NOT NULL DEFAULT 0)");
            handle.execute("INSERT INTO _table_versions (table_name, version) VALUES ('player_sessions', 1)");
        });

        assertTrue(oldDatabase.registerTable(new PlayerSessionsTable()));
        Database.PlayerLease lease = oldDatabase.acquirePlayerSession(UUID.randomUUID(), "A", 60_000).orElseThrow();
        assertEquals(1, lease.fenceToken());
        assertEquals(0, lease.dataRevision());
    }

    @Test
    void databaseMigrationSkipsWholeExistingPlayer() {
        Jdbi source = Jdbi.create("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        Jdbi target = Jdbi.create("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        source.installPlugin(new SqlObjectPlugin());
        target.installPlugin(new SqlObjectPlugin());
        source.useHandle(handle -> {
            handle.execute(new SkillsTable().getCreateCommand());
            handle.attach(SkillsDao.class).upsertSkills(List.of(
                    new SkillRow(uuid.toString(), "mining", 1_000, 0, 1_000),
                    new SkillRow(uuid.toString(), "combat", 1_000, 0, 1_000)));
        });
        target.useHandle(handle -> {
            handle.execute(new SkillsTable().getCreateCommand());
            handle.attach(SkillsDao.class).upsertSkills(List.of(
                    new SkillRow(uuid.toString(), "mining", 5_000, 0, 5_000)));
        });

        assertEquals(0, DbMigrator.migrateSkills(source, target));
        List<SkillRow> result = target.withHandle(handle -> handle.attach(SkillsDao.class)
                .getSkills(uuid.toString()));
        assertEquals(1, result.size());
        assertEquals(5_000, result.getFirst().getTotalXp());
    }

    @Test
    void concurrentSchemaUpgradeRunsOnce() throws Exception {
        AtomicInteger upgrades = new AtomicInteger();
        database.registerTable(versionedTable(1, upgrades));
        Database otherServer = otherDatabase;
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                await(start);
                return database.registerTable(versionedTable(2, upgrades));
            });
            var second = executor.submit(() -> {
                await(start);
                return otherServer.registerTable(versionedTable(2, upgrades));
            });
            start.countDown();
            assertTrue(first.get(5, TimeUnit.SECONDS));
            assertTrue(second.get(5, TimeUnit.SECONDS));
        }
        assertEquals(1, upgrades.get());
        int version = jdbi.withHandle(handle -> handle.createQuery(
                        "SELECT version FROM _table_versions WHERE table_name = 'concurrent_test'")
                .mapTo(Integer.class).one());
        assertEquals(2, version);
    }

    @Test
    void h2MigrationLockDoesNotReclaimNonNullOwner() throws Exception {
        Assumptions.assumeTrue("H2".equals(jdbi.withHandle(handle ->
                handle.getConnection().getMetaData().getDatabaseProductName())));
        jdbi.useHandle(handle -> handle.createUpdate(
                        "UPDATE _migration_lock SET owner = 'abandoned', lock_timestamp = 0 WHERE lock_name = :name")
                .bind("name", "cavecrawlers_schema_migration")
                .execute());

        try (var executor = Executors.newSingleThreadExecutor()) {
            var migration = executor.submit(() -> database.registerTable(versionedTable(1, new AtomicInteger())));
            assertThrows(TimeoutException.class, () -> migration.get(250, TimeUnit.MILLISECONDS));
            migration.cancel(true);
        }

        String owner = jdbi.withHandle(handle -> handle.createQuery(
                        "SELECT owner FROM _migration_lock WHERE lock_name = :name")
                .bind("name", "cavecrawlers_schema_migration")
                .mapTo(String.class)
                .one());
        assertEquals("abandoned", owner);
    }

    @Test
    void lostMigrationLeaseRollsBackVersionPublication() throws Exception {
        Assumptions.assumeTrue("H2".equals(jdbi.withHandle(handle ->
                handle.getConnection().getMetaData().getDatabaseProductName())));
        database.registerTable(versionedTable(1, new AtomicInteger()));
        CountDownLatch migrationStarted = new CountDownLatch(1);
        Database fastRefresh = new Database(jdbi, false, 10);
        SqlTable delayedUpgrade = new SqlTable() {
            @Override public String getTableName() { return "concurrent_test"; }
            @Override public String getCreateCommand() { return "CREATE TABLE concurrent_test (id INT PRIMARY KEY)"; }
            @Override public int getVersion() { return 2; }
            @Override public void onCreate(Handle handle) { handle.execute(getCreateCommand()); }

            @Override
            public void onUpgrade(Handle handle, int oldVersion, int newVersion) {
                migrationStarted.countDown();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }
        };

        try (var executor = Executors.newSingleThreadExecutor()) {
            var migration = executor.submit(() -> fastRefresh.registerTable(delayedUpgrade));
            assertTrue(migrationStarted.await(5, TimeUnit.SECONDS));
            jdbi.useHandle(handle -> handle.createUpdate(
                            "UPDATE _migration_lock SET owner = 'stolen' WHERE lock_name = :name")
                    .bind("name", "cavecrawlers_schema_migration")
                    .execute());
            assertThrows(ExecutionException.class, () -> migration.get(5, TimeUnit.SECONDS));
        }

        int version = jdbi.withHandle(handle -> handle.createQuery(
                        "SELECT version FROM _table_versions WHERE table_name = 'concurrent_test'")
                .mapTo(Integer.class).one());
        assertEquals(1, version);
    }

    @Test
    void legacyImportCannotResurrectResetOrClaimedEmptyPlayer() {
        assertTrue(database.resetOfflinePlayer(uuid, 60_000));
        assertFalse(database.importLegacySkills(uuid, rows(900)));
        assertEquals(0, totalXp());
        uuid = UUID.randomUUID();
        acquire("A");
        assertFalse(otherDatabase.importLegacySkills(uuid, rows(900)));
        assertEquals(0, totalXp());
    }

    @Test
    void releasedSameFenceIsReportedAsLostOwnership() {
        var lease = acquire("A");
        database.releasePlayerSession(uuid, "A", lease.fenceToken());
        var outcome = database.persistPlayer(uuid, "A", lease.fenceToken(), 1, rows(100), false, false);
        assertFalse(outcome.committed());
        assertFalse(outcome.owned());
    }

    @Test
    void snapshotCannotWriteAnotherPlayersRows() {
        var lease = acquire("A");
        assertThrows(IllegalArgumentException.class, () -> database.persistPlayer(uuid, "A", lease.fenceToken(),
                1, List.of(new SkillRow(UUID.randomUUID().toString(), "mining", 9, 0, 9)), false, false));
        assertEquals(0, dataRevision());
    }

    protected Database.PlayerLease acquire(String server) {
        return (server.equals("B") ? otherDatabase : database).acquirePlayerSession(uuid, server, 60_000).orElseThrow();
    }

    protected void expireLease() {
        jdbi.useHandle(handle -> handle.createUpdate(
                        "UPDATE player_sessions SET lock_timestamp = 0 WHERE player_uuid = :uuid")
                .bind("uuid", uuid.toString()).execute());
    }

    protected List<SkillRow> rows(double totalXp) {
        return List.of(new SkillRow(uuid.toString(), "mining", totalXp, 0, totalXp));
    }

    protected double totalXp() {
        return jdbi.withHandle(handle -> handle.createQuery(
                        "SELECT total_xp FROM skills WHERE player_uuid = :uuid AND type = 'mining'")
                .bind("uuid", uuid.toString()).mapTo(Double.class).findOne().orElse(0D));
    }

    protected String lockingServer() {
        return jdbi.withHandle(handle -> handle.createQuery(
                        "SELECT locking_server FROM player_sessions WHERE player_uuid = :uuid")
                .bind("uuid", uuid.toString()).mapTo(String.class).one());
    }

    protected long dataRevision() {
        return jdbi.withHandle(handle -> handle.createQuery(
                        "SELECT data_revision FROM player_sessions WHERE player_uuid = :uuid")
                .bind("uuid", uuid.toString()).mapTo(Long.class).one());
    }

    protected long fenceToken() {
        return jdbi.withHandle(handle -> handle.createQuery(
                        "SELECT fence_token FROM player_sessions WHERE player_uuid = :uuid")
                .bind("uuid", uuid.toString()).mapTo(Long.class).one());
    }

    protected int isLocked() {
        return jdbi.withHandle(handle -> handle.createQuery(
                        "SELECT is_locked FROM player_sessions WHERE player_uuid = :uuid")
                .bind("uuid", uuid.toString()).mapTo(Integer.class).one());
    }

    private int addonPayload() {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT payload FROM addon_test WHERE player_uuid=:uuid")
                .bind("uuid", uuid.toString()).mapTo(Integer.class).one());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static SqlTable versionedTable(int version, AtomicInteger upgrades) {
        return new SqlTable() {
            @Override
            public String getTableName() {
                return "concurrent_test";
            }

            @Override
            public String getCreateCommand() {
                return "CREATE TABLE concurrent_test (id INT PRIMARY KEY)";
            }

            @Override
            public int getVersion() {
                return version;
            }

            @Override
            public void onCreate(Handle handle) {
                handle.execute(getCreateCommand());
            }

            @Override
            public void onUpgrade(Handle handle, int oldVersion, int newVersion) {
                upgrades.incrementAndGet();
                handle.execute("ALTER TABLE concurrent_test ADD COLUMN payload INT DEFAULT 0");
            }
        };
    }

    private static final class ToggleAddonTable extends PlayerDataSqlTable {
        private final AtomicBoolean fail = new AtomicBoolean();

        @Override
        public String getTableName() {
            return "addon_test";
        }

        @Override
        public String getCreateCommand() {
            return "CREATE TABLE addon_test (player_uuid VARCHAR(36) PRIMARY KEY, payload INT NOT NULL)";
        }

        @Override
        public int getVersion() {
            return 1;
        }

        @Override
        public void onCreate(Handle handle) {
            handle.execute(getCreateCommand());
        }

        @Override
        public void onUpgrade(Handle handle, int oldVersion, int newVersion) {
        }

        @Override
        public void loadForPlayer(Handle handle, UUID playerUuid) {
        }

        @Override
        public void saveForPlayer(Handle handle, UUID playerUuid) {
            handle.createUpdate("INSERT INTO addon_test (player_uuid, payload) VALUES (:uuid, 1) " +
                            "ON DUPLICATE KEY UPDATE payload = payload + 1")
                    .bind("uuid", playerUuid.toString()).execute();
            if (fail.get()) {
                throw new IllegalStateException("simulated addon failure");
            }
        }
    }

    private static final class OrderedAddonTable extends PlayerDataSqlTable {
        private final String name;
        private final List<String> order;

        private OrderedAddonTable(String name, List<String> order) {
            this.name = name;
            this.order = order;
        }

        @Override public String getTableName() { return "ordered_" + name; }
        @Override public String getCreateCommand() {
            return "CREATE TABLE ordered_" + name + " (player_uuid VARCHAR(36) PRIMARY KEY)";
        }
        @Override public int getVersion() { return 1; }
        @Override public void onCreate(Handle handle) { handle.execute(getCreateCommand()); }
        @Override public void onUpgrade(Handle handle, int oldVersion, int newVersion) { }
        @Override public void loadForPlayer(Handle handle, UUID playerUuid) { }
        @Override public void saveForPlayer(Handle handle, UUID playerUuid) { order.add(name); }
    }
}
