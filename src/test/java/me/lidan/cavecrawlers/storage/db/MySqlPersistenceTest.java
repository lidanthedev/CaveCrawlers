package me.lidan.cavecrawlers.storage.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/** Inherits the persistence contract and executes it against two independent backend pools. */
@Testcontainers(disabledWithoutDocker = true)
class MySqlPersistenceTest extends DatabasePersistenceTest {
    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4.8");
    private HikariDataSource poolA;
    private HikariDataSource poolB;
    private Jdbi otherJdbi;
    private static final String LOCK = "cavecrawlers_schema_migration";

    @Override
    @BeforeEach
    void setUp() {
        poolA = pool("backend-A");
        poolB = pool("backend-B");
        jdbi = DbMigrator.toJdbi(poolA);
        otherJdbi = DbMigrator.toJdbi(poolB);
        jdbi.useHandle(h -> {
            for (String table : h.createQuery("SHOW TABLES").mapTo(String.class).list()) h.execute("DROP TABLE `" + table + "`");
        });
        database = new Database(jdbi, true);
        otherDatabase = new Database(otherJdbi, true);
        database.registerTable(new SkillsTable());
        database.registerTable(new PlayerSessionsTable());
        uuid = UUID.randomUUID();
        long a = jdbi.withHandle(h -> h.createQuery("SELECT CONNECTION_ID()").mapTo(Long.class).one());
        long b = otherJdbi.withHandle(h -> h.createQuery("SELECT CONNECTION_ID()").mapTo(Long.class).one());
        assertNotEquals(a, b);
    }

    private HikariDataSource pool(String name) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(MYSQL.getJdbcUrl() + (MYSQL.getJdbcUrl().contains("?") ? "&" : "?") + "socketTimeout=10000");
        config.setUsername(MYSQL.getUsername());
        config.setPassword(MYSQL.getPassword());
        config.setPoolName(name);
        config.setMaximumPoolSize(3);
        return new HikariDataSource(config);
    }

    @AfterEach
    void closePools() {
        if (poolA != null) poolA.close();
        if (poolB != null) poolB.close();
    }

    @Override
    @Test
    void playerSessionVersionOneUpgradesInPlace() {
        jdbi.useHandle(h -> {
            h.execute("DROP TABLE player_sessions");
            h.execute("CREATE TABLE player_sessions (player_uuid VARCHAR(36) PRIMARY KEY, "
                    + "is_locked TINYINT NOT NULL DEFAULT 0, locking_server VARCHAR(64), lock_timestamp BIGINT NOT NULL DEFAULT 0)");
            h.execute("UPDATE _table_versions SET version=1 WHERE table_name='player_sessions'");
        });
        assertTrue(database.registerTable(new PlayerSessionsTable()));
        var lease = acquire("A");
        assertEquals(1, lease.fenceToken());
        assertEquals(0, lease.dataRevision());
    }

    @Override
    @Test
    void databaseMigrationSkipsWholeExistingPlayer() {
        // H2 -> MySQL is the production migration command; the target is the real shared MySQL DB.
        Jdbi source = Jdbi.create("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        source.installPlugin(new org.jdbi.v3.sqlobject.SqlObjectPlugin());
        source.useHandle(h -> {
            h.execute(new SkillsTable().getCreateCommand());
            h.attach(SkillsDao.class).upsertSkills(java.util.List.of(
                    new SkillRow(uuid.toString(), "mining", 100, 0, 100),
                    new SkillRow(uuid.toString(), "combat", 100, 0, 100)));
        });
        jdbi.useHandle(h -> h.attach(SkillsDao.class).upsertSkills(rows(500)));
        assertEquals(0, DbMigrator.migrateSkills(source, otherJdbi));
        assertEquals(500, totalXp());
        assertEquals(1, jdbi.withHandle(h -> h.attach(SkillsDao.class).getSkills(uuid.toString())).size());
    }

    @Test
    void takeoverWaitsForSaveRowLock() throws Exception {
        saveWhileAcquiring(false);
    }

    @Test
    void quitPublishesFinalDataBeforeReleasingRowLock() throws Exception {
        saveWhileAcquiring(true);
    }

    private void saveWhileAcquiring(boolean quit) throws Exception {
        var lease = acquire("A");
        if (!quit) expireLease();
        CountDownLatch writing = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        database.registerTable(new PlayerDataSqlTable() {
            public String getTableName() { return "blocking_addon"; }
            public int getVersion() { return 1; }
            public String getCreateCommand() { return "CREATE TABLE blocking_addon (id INT)"; }
            public void onCreate(Handle h) { h.execute(getCreateCommand()); }
            public void onUpgrade(Handle h, int old, int next) { }
            public void loadForPlayer(Handle h, UUID id) { }
            public void saveForPlayer(Handle h, UUID id) { writing.countDown(); await(finish); }
        });
        try (var executor = Executors.newFixedThreadPool(2)) {
            var save = executor.submit(() -> database.persistPlayer(uuid, "A", lease.fenceToken(), 1, rows(1234), false, quit));
            try {
                assertTrue(writing.await(5, TimeUnit.SECONDS));
                CountDownLatch acquiring = new CountDownLatch(1);
                var takeover = executor.submit(() -> { acquiring.countDown(); return acquire("B"); });
                assertTrue(acquiring.await(5, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> takeover.get(200, TimeUnit.MILLISECONDS));
                finish.countDown();
                assertTrue(save.get(5, TimeUnit.SECONDS).committed());
                var next = takeover.get(5, TimeUnit.SECONDS);
                assertTrue(next.fenceToken() > lease.fenceToken());
                assertEquals(1234, otherDatabase.loadPlayer(uuid, "B", next.fenceToken()).getFirst().getTotalXp());
            } finally { finish.countDown(); }
        }
    }

    @Test
    void saveWaitsForTakeoverThenRejectsOldFence() throws Exception {
        var old = acquire("A");
        expireLease();
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        otherJdbi.setSqlLogger(new org.jdbi.v3.core.statement.SqlLogger() {
            @Override public void logAfterExecution(org.jdbi.v3.core.statement.StatementContext context) {
                if (context.getRawSql().startsWith("UPDATE player_sessions SET is_locked = 1")) {
                    locked.countDown(); await(finish);
                }
            }
        });
        try (var executor = Executors.newFixedThreadPool(2)) {
            var takeover = executor.submit(() -> acquire("B"));
            try {
                assertTrue(locked.await(5, TimeUnit.SECONDS));
                var stale = executor.submit(() -> database.persistPlayer(uuid, "A", old.fenceToken(), 1, rows(100), false, false));
                assertThrows(TimeoutException.class, () -> stale.get(200, TimeUnit.MILLISECONDS));
                finish.countDown();
                var next = takeover.get(5, TimeUnit.SECONDS);
                assertFalse(stale.get(5, TimeUnit.SECONDS).committed());
                assertTrue(otherDatabase.persistPlayer(uuid, "B", next.fenceToken(), 1, rows(200), false, false).committed());
                assertEquals(200, totalXp());
            } finally { finish.countDown(); }
        }
    }

    @Test
    void migrationFailureKeepsVersionAndReleasesSamePhysicalConnectionLock() {
        AtomicLong connectionId = new AtomicLong();
        SqlTable broken = migrationTable(connectionId, true);
        assertThrows(RuntimeException.class, () -> database.registerTable(broken));
        assertEquals(0, jdbi.withHandle(h -> h.createQuery("SELECT COUNT(*) FROM _table_versions WHERE table_name='migration_test'")
                .mapTo(Integer.class).one()).intValue());
        assertTrue(connectionId.get() > 0);
        assertLockFreeFromOtherPool();
        assertTrue(otherDatabase.registerTable(migrationTable(connectionId, false)));
        assertLockFreeFromOtherPool();
    }

    private SqlTable migrationTable(AtomicLong id, boolean fail) {
        return new SqlTable() {
            public String getTableName() { return "migration_test"; }
            public int getVersion() { return 1; }
            public String getCreateCommand() { return "CREATE TABLE IF NOT EXISTS migration_test (id INT PRIMARY KEY)"; }
            public void onCreate(Handle h) {
                long connection = h.createQuery("SELECT CONNECTION_ID()").mapTo(Long.class).one();
                id.set(connection);
                assertEquals(connection, h.createQuery("SELECT IS_USED_LOCK(:name)").bind("name", LOCK).mapTo(Long.class).one());
                h.execute(getCreateCommand()); // MySQL implicitly commits DDL. Retry must tolerate it.
                assertEquals(connection, h.createQuery("SELECT CONNECTION_ID()").mapTo(Long.class).one());
                assertEquals(connection, h.createQuery("SELECT IS_USED_LOCK(:name)").bind("name", LOCK).mapTo(Long.class).one());
                if (fail) throw new IllegalStateException("intentional migration failure");
            }
            public void onUpgrade(Handle h, int old, int next) { }
        };
    }

    @Test
    void physicalConnectionCloseReleasesAdvisoryLockEvenAfterRollback() throws Exception {
        try (var connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())) {
            try (var statement = connection.createStatement()) {
                try (var result = statement.executeQuery("SELECT GET_LOCK('" + LOCK + "', 0)")) {
                    assertTrue(result.next()); assertEquals(1, result.getInt(1));
                }
                connection.setAutoCommit(false);
                connection.rollback();
                otherJdbi.useHandle(h -> assertEquals(0, h.createQuery("SELECT GET_LOCK(:name, 0)")
                        .bind("name", LOCK).mapTo(Integer.class).one()));
            }
        }
        assertLockFreeFromOtherPool();
    }

    private void assertLockFreeFromOtherPool() {
        otherJdbi.useHandle(h -> {
            assertEquals(1, h.createQuery("SELECT GET_LOCK(:name, 0)").bind("name", LOCK).mapTo(Integer.class).one());
            assertEquals(1, h.createQuery("SELECT RELEASE_LOCK(:name)").bind("name", LOCK).mapTo(Integer.class).one());
        });
    }

    @Test
    void failedUpgradeDoesNotPublishVersionAndCanRetryAfterImplicitDdlCommit() {
        SqlTable table = new SqlTable() {
            public String getTableName() { return "upgrade_failure"; }
            public int getVersion() { return 1; }
            public String getCreateCommand() { return "CREATE TABLE upgrade_failure (id INT)"; }
            public void onCreate(Handle h) { h.execute(getCreateCommand()); }
            public void onUpgrade(Handle h, int old, int next) { }
        };
        database.registerTable(table);
        AtomicInteger attempts = new AtomicInteger();
        SqlTable upgrade = new SqlTable() {
            public String getTableName() { return "upgrade_failure"; }
            public int getVersion() { return 2; }
            public String getCreateCommand() { return table.getCreateCommand(); }
            public void onCreate(Handle h) { table.onCreate(h); }
            public void onUpgrade(Handle h, int old, int next) {
                h.execute("CREATE TABLE IF NOT EXISTS upgrade_side_effect (id INT)");
                if (attempts.incrementAndGet() == 1) throw new IllegalStateException("failed after implicit DDL commit");
            }
        };
        assertThrows(RuntimeException.class, () -> database.registerTable(upgrade));
        assertEquals(1, jdbi.withHandle(h -> h.createQuery("SELECT version FROM _table_versions WHERE table_name='upgrade_failure'")
                .mapTo(Integer.class).one()).intValue());
        assertLockFreeFromOtherPool();
        assertTrue(otherDatabase.registerTable(upgrade));
        assertEquals(2, jdbi.withHandle(h -> h.createQuery("SELECT version FROM _table_versions WHERE table_name='upgrade_failure'")
                .mapTo(Integer.class).one()).intValue());
        assertLockFreeFromOtherPool();
    }

    private static void await(CountDownLatch latch) {
        try { assertTrue(latch.await(10, TimeUnit.SECONDS)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
    }
}
