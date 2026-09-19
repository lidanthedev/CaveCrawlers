package me.lidan.cavecrawlers.storage.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
public class Database {
    private static final String MIGRATION_LOCK = "cavecrawlers_schema_migration";
    private static final long MIGRATION_REFRESH_MILLIS = 60_000;
    private static Database instance;

    private HikariDataSource dataSource;
    @Getter
    private Jdbi jdbi;
    private final CopyOnWriteArrayList<PlayerDataSqlTable> playerDataTables = new CopyOnWriteArrayList<>();
    private volatile boolean mysql;
    @Getter
    private volatile boolean available;
    @Getter
    private volatile boolean isInitialized;
    private final long migrationRefreshMillis;

    private Database() {
        migrationRefreshMillis = MIGRATION_REFRESH_MILLIS;
    }

    Database(Jdbi jdbi, boolean mysql) {
        this(jdbi, mysql, MIGRATION_REFRESH_MILLIS);
    }

    Database(Jdbi jdbi, boolean mysql, long migrationRefreshMillis) {
        if (migrationRefreshMillis <= 0) throw new IllegalArgumentException("Migration refresh must be positive");
        this.jdbi = jdbi;
        this.mysql = mysql;
        this.migrationRefreshMillis = migrationRefreshMillis;
        this.available = true;
        this.isInitialized = true;
        initializeMetadataTables();
    }

    public static synchronized Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    public static HikariDataSource openMysqlSource(Plugin plugin, int poolSize) {
        FileConfiguration config = plugin.getConfig();
        DBConnectionInfo result = getDbConnectionInfo(config);
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(buildMysqlJdbcUrl(config, result));
        hikariConfig.setUsername(result.username());
        hikariConfig.setPassword(result.password());
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setMaximumPoolSize(poolSize);
        hikariConfig.setConnectionTimeout(10000);
        hikariConfig.setPoolName("CaveCrawlers-MySQL-Migration");
        return new HikariDataSource(hikariConfig);
    }

    /** Schema DDL and version publication run under one cluster-wide lock. */
    public boolean registerTable(SqlTable table) {
        if (!available || jdbi == null) {
            return false;
        }

        withMigrationLock(handle -> {
            Optional<Integer> storedVersion = handle
                    .createQuery("SELECT version FROM _table_versions WHERE table_name = :name")
                    .bind("name", table.getTableName())
                    .mapTo(Integer.class)
                    .findFirst();

            if (storedVersion.isEmpty()) {
                table.onCreate(handle);
                handle.createUpdate("INSERT INTO _table_versions (table_name, version) VALUES (:name, :version)")
                        .bind("name", table.getTableName())
                        .bind("version", table.getVersion())
                        .execute();
                log.info("Created table '{}' at version {}", table.getTableName(), table.getVersion());
            } else if (storedVersion.get() > table.getVersion()) {
                throw new IllegalStateException("Database schema is newer than this plugin: " + table.getTableName());
            } else if (storedVersion.get() < table.getVersion()) {
                int oldVersion = storedVersion.get();
                table.onUpgrade(handle, oldVersion, table.getVersion());
                handle.createUpdate("UPDATE _table_versions SET version = :version WHERE table_name = :name")
                        .bind("name", table.getTableName())
                        .bind("version", table.getVersion())
                        .execute();
                log.info("Upgraded table '{}' from version {} to {}", table.getTableName(), oldVersion, table.getVersion());
            }
        });

        if (table instanceof PlayerDataSqlTable playerDataTable) {
            playerDataTables.removeIf(registered -> registered.getTableName().equals(playerDataTable.getTableName()));
            playerDataTables.add(playerDataTable);
        }
        return true;
    }

    private void withMigrationLock(Consumer<Handle> migration) {
        if (mysql) {
            jdbi.useHandle(handle -> {
                boolean acquired = handle.createQuery("SELECT GET_LOCK(:name, 60)")
                        .bind("name", MIGRATION_LOCK)
                        .mapTo(Boolean.class)
                        .one();
                if (!acquired) {
                    throw new IllegalStateException("Timed out acquiring database schema migration lock");
                }
                try {
                    handle.useTransaction(migration::accept);
                } finally {
                    try {
                        Integer released = handle.createQuery("SELECT RELEASE_LOCK(:name)")
                                .bind("name", MIGRATION_LOCK).mapTo(Integer.class).one();
                        if (!Integer.valueOf(1).equals(released)) {
                            throw new IllegalStateException("Migration advisory lock was not held by this connection");
                        }
                    } catch (Exception e) {
                        // A pooled close does not close the physical connection or release GET_LOCK.
                        try { handle.getConnection().abort(Runnable::run); }
                        catch (Exception abortFailure) { e.addSuppressed(abortFailure); }
                        throw e;
                    }
                }
            });
            return;
        }

        String owner = UUID.randomUUID().toString();
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(60).toNanos();
        boolean acquired = false;
        while (System.nanoTime() < deadline) {
            // A non-null owner is cleared only by its owner or by offline recovery after every DB user stops.
            int updated = jdbi.withHandle(handle -> handle.createUpdate(
                            "UPDATE _migration_lock SET owner = :owner, lock_timestamp = " + databaseNowExpression()
                                    + " WHERE lock_name = :name AND owner IS NULL")
                    .bind("owner", owner)
                    .bind("name", MIGRATION_LOCK)
                    .execute());
            if (updated == 1) {
                acquired = true;
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while acquiring schema migration lock", e);
            }
        }
        if (!acquired) {
            throw new IllegalStateException("Timed out acquiring database schema migration lock");
        }
        AtomicBoolean refreshing = new AtomicBoolean(true);
        AtomicReference<Handle> migrationHandle = new AtomicReference<>();
        AtomicReference<RuntimeException> refreshFailure = new AtomicReference<>();
        ScheduledExecutorService refresher = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "CaveCrawlers-H2-Migration-Lease");
            thread.setDaemon(true);
            return thread;
        });
        ScheduledFuture<?> refreshTask = refresher.scheduleAtFixedRate(
                () -> refreshMigrationLease(owner, refreshing, migrationHandle, refreshFailure),
                migrationRefreshMillis, migrationRefreshMillis, TimeUnit.MILLISECONDS);
        try {
            jdbi.useHandle(handle -> {
                migrationHandle.set(handle);
                try {
                    handle.useTransaction(transaction -> {
                        migration.accept(transaction);
                        throwRefreshFailure(refreshFailure);
                        refreshMigrationLease(owner);
                        throwRefreshFailure(refreshFailure);
                    });
                    refreshing.set(false);
                } finally {
                    migrationHandle.compareAndSet(handle, null);
                }
            });
        } finally {
            refreshing.set(false);
            refreshTask.cancel(true);
            refresher.shutdownNow();
            jdbi.useHandle(handle -> handle.createUpdate(
                            "UPDATE _migration_lock SET owner = NULL, lock_timestamp = 0 " +
                                    "WHERE lock_name = :name AND owner = :owner")
                    .bind("name", MIGRATION_LOCK)
                    .bind("owner", owner)
                    .execute());
        }
    }

    private void refreshMigrationLease(String owner, AtomicBoolean refreshing,
                                       AtomicReference<Handle> migrationHandle,
                                       AtomicReference<RuntimeException> refreshFailure) {
        try {
            refreshMigrationLease(owner);
        } catch (RuntimeException e) {
            if (!refreshing.get()) return;
            IllegalStateException failure = new IllegalStateException("Lost database schema migration lock", e);
            if (refreshFailure.compareAndSet(null, failure)) {
                Handle handle = migrationHandle.get();
                if (handle != null) {
                    try { handle.getConnection().abort(Runnable::run); }
                    catch (Exception abortFailure) { failure.addSuppressed(abortFailure); }
                }
            }
        }
    }

    private void refreshMigrationLease(String owner) {
        int refreshed = jdbi.withHandle(handle -> handle.createUpdate(
                        "UPDATE _migration_lock SET lock_timestamp = " + databaseNowExpression()
                                + " WHERE lock_name = :name AND owner = :owner")
                .bind("name", MIGRATION_LOCK)
                .bind("owner", owner)
                .execute());
        if (refreshed != 1) throw new IllegalStateException("Database schema migration lock is no longer owned");
    }

    private static void throwRefreshFailure(AtomicReference<RuntimeException> refreshFailure) {
        RuntimeException failure = refreshFailure.get();
        if (failure != null) throw failure;
    }

    /** Acquires or refreshes a lease while holding the session row lock. */
    public Optional<PlayerLease> acquirePlayerSession(UUID uuid, String serverId, long leaseTimeoutMillis) {
        String uuidString = uuid.toString();
        return jdbi.inTransaction(handle -> {
            ensureSessionRow(handle, uuidString);
            SessionRow row = lockSessionRow(handle, uuidString);
            long now = databaseNowMillis(handle);
            boolean sameLiveOwner = row.locked() && serverId.equals(row.serverId())
                    && row.lockTimestamp() >= now - leaseTimeoutMillis;
            if (row.locked() && !sameLiveOwner && row.lockTimestamp() >= now - leaseTimeoutMillis) {
                return Optional.empty();
            }

            long fence = sameLiveOwner ? row.fenceToken() : Math.addExact(row.fenceToken(), 1L);
            handle.createUpdate("UPDATE player_sessions SET is_locked = 1, locking_server = :server, " +
                            "lock_timestamp = :now, fence_token = :fence WHERE player_uuid = :uuid")
                    .bind("server", serverId)
                    .bind("now", now)
                    .bind("fence", fence)
                    .bind("uuid", uuidString)
                    .execute();
            return Optional.of(new PlayerLease(fence, row.dataRevision()));
        });
    }

    /** Uses database time, so host clock skew cannot expire another server's lease. */
    public int heartbeatAll(String serverId) {
        return jdbi.withHandle(handle -> handle.createUpdate(
                        "UPDATE player_sessions SET lock_timestamp = " + databaseNowExpression()
                                + " WHERE is_locked = 1 AND locking_server = :server")
                .bind("server", serverId)
                .execute());
    }

    public int releasePlayerSession(UUID uuid, String serverId, long fenceToken) {
        return jdbi.withHandle(handle -> handle.attach(PlayerSessionsDao.class)
                .releaseLock(uuid.toString(), serverId, fenceToken));
    }

    public void releaseAllLocks(String serverId) {
        jdbi.useHandle(handle -> handle.attach(PlayerSessionsDao.class).releaseAllLocks(serverId));
    }

    /** Loads all required player tables from one ownership-validated transaction. */
    public List<SkillRow> loadPlayer(UUID uuid, String serverId, long fenceToken) {
        return jdbi.inTransaction(handle -> {
            SessionRow row = lockSessionRow(handle, uuid.toString());
            if (!row.ownedBy(serverId, fenceToken)) {
                throw new StaleSessionException(uuid, fenceToken, row.fenceToken());
            }
            List<SkillRow> rows = handle.attach(SkillsDao.class).getSkills(uuid.toString());
            for (PlayerDataSqlTable table : playerDataTables) {
                table.loadForPlayer(handle, uuid);
            }
            return rows;
        });
    }

    /**
     * Fencing validation and every player-data write share this transaction and
     * locked session row. Old owners cannot commit after a new owner acquires it.
     */
    public WriteOutcome persistPlayer(UUID uuid, String serverId, long fenceToken, long revision,
                                      List<SkillRow> rows, boolean deleteBeforeWrite,
                                      boolean releaseAfterWrite) {
        if (rows.stream().anyMatch(row -> !uuid.toString().equals(row.getPlayerUuid()))) {
            throw new IllegalArgumentException("Snapshot contains another player's rows");
        }
        return jdbi.inTransaction(handle -> {
            SessionRow session = lockSessionRow(handle, uuid.toString());
            if (!session.ownedBy(serverId, fenceToken) || revision <= session.dataRevision()) {
                return new WriteOutcome(false, session.fenceToken(), session.dataRevision(), session.ownedBy(serverId, fenceToken));
            }

            SkillsDao skills = handle.attach(SkillsDao.class);
            if (deleteBeforeWrite) {
                skills.deleteSkills(uuid.toString());
                for (PlayerDataSqlTable table : playerDataTables) {
                    table.resetForPlayer(handle, uuid);
                }
            }
            if (!rows.isEmpty()) {
                skills.upsertSkills(rows);
            }
            for (PlayerDataSqlTable table : playerDataTables) {
                table.saveForPlayer(handle, uuid);
            }

            int updated = handle.createUpdate("UPDATE player_sessions SET data_revision = :revision " +
                            "WHERE player_uuid = :uuid AND is_locked = 1 " +
                            "AND locking_server = :server AND fence_token = :fence")
                    .bind("revision", revision)
                    .bind("uuid", uuid.toString())
                    .bind("server", serverId)
                    .bind("fence", fenceToken)
                    .execute();
            if (updated != 1) {
                throw new IllegalStateException("Ownership changed while the session row was locked");
            }
            if (releaseAfterWrite) {
                handle.attach(PlayerSessionsDao.class)
                        .releaseLock(uuid.toString(), serverId, fenceToken);
            }
            return new WriteOutcome(true, fenceToken, revision, true);
        });
    }

    /** Offline reset. A live owner wins; an expired owner is fenced out atomically. */
    public boolean resetOfflinePlayer(UUID uuid, long leaseTimeoutMillis) {
        return jdbi.inTransaction(handle -> {
            String uuidString = uuid.toString();
            ensureSessionRow(handle, uuidString);
            SessionRow row = lockSessionRow(handle, uuidString);
            long now = databaseNowMillis(handle);
            if (row.locked() && row.lockTimestamp() >= now - leaseTimeoutMillis) {
                return false;
            }
            handle.attach(SkillsDao.class).deleteSkills(uuidString);
            for (PlayerDataSqlTable table : playerDataTables) {
                table.resetForPlayer(handle, uuid);
            }
            handle.createUpdate("UPDATE player_sessions SET is_locked = 0, locking_server = NULL, " +
                            "lock_timestamp = 0, fence_token = :fence, data_revision = :revision " +
                            "WHERE player_uuid = :uuid")
                    .bind("fence", Math.addExact(row.fenceToken(), 1L))
                    .bind("revision", Math.addExact(row.dataRevision(), 1L))
                    .bind("uuid", uuidString)
                    .execute();
            return true;
        });
    }

    /** Idempotent and non-destructive legacy import. */
    public boolean importLegacySkills(UUID uuid, List<SkillRow> rows) {
        if (rows.stream().anyMatch(row -> !uuid.toString().equals(row.getPlayerUuid()))) {
            throw new IllegalArgumentException("Import contains another player's rows");
        }
        return jdbi.inTransaction(handle -> {
            ensureSessionRow(handle, uuid.toString());
            SessionRow session = lockSessionRow(handle, uuid.toString());
            if (session.locked() || session.fenceToken() > 0 || session.dataRevision() > 0) return false;
            int claimed = handle.createUpdate("INSERT IGNORE INTO _legacy_player_migrations (player_uuid) VALUES (:uuid)")
                    .bind("uuid", uuid.toString())
                    .execute();
            if (claimed == 0) {
                return false;
            }
            SkillsDao skills = handle.attach(SkillsDao.class);
            if (!skills.hasSkills(uuid.toString()) && !rows.isEmpty()) {
                skills.insertSkillsIfAbsent(rows);
                return true;
            }
            return false;
        });
    }

    private void ensureSessionRow(Handle handle, String uuid) {
        handle.attach(PlayerSessionsDao.class).ensureRow(uuid);
    }

    private SessionRow lockSessionRow(Handle handle, String uuid) {
        return handle.createQuery("SELECT is_locked, locking_server, lock_timestamp, fence_token, data_revision " +
                        "FROM player_sessions WHERE player_uuid = :uuid FOR UPDATE")
                .bind("uuid", uuid)
                .map((rs, ctx) -> new SessionRow(
                        rs.getBoolean("is_locked"), rs.getString("locking_server"),
                        rs.getLong("lock_timestamp"), rs.getLong("fence_token"),
                        rs.getLong("data_revision")))
                .one();
    }

    private long databaseNowMillis(Handle handle) {
        return handle.createQuery("SELECT " + databaseNowExpression()).mapTo(Long.class).one();
    }

    private String databaseNowExpression() {
        return mysql
                ? "CAST(UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000 AS SIGNED)"
                : "DATEDIFF('MILLISECOND', TIMESTAMP '1970-01-01 00:00:00', CURRENT_TIMESTAMP)";
    }

    public synchronized void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        dataSource = null;
        jdbi = null;
        available = false;
    }

    private static @NonNull DBConnectionInfo getDbConnectionInfo(FileConfiguration config) {
        return new DBConnectionInfo(config.getString("database.host", "localhost"),
                config.getInt("database.port", 3306),
                config.getString("database.database", "cavecrawlers"),
                config.getString("database.username", "root"),
                config.getString("database.password", ""));
    }

    private static String buildMysqlJdbcUrl(FileConfiguration config, DBConnectionInfo info) {
        String sslMode = config.getString("database.sslMode", "DISABLED");
        boolean allowPublicKeyRetrieval = config.getBoolean("database.allow-public-key-retrieval", false);
        return "jdbc:mysql://" + info.host() + ":" + info.port() + "/" + info.database()
                + "?sslMode=" + sslMode + "&allowPublicKeyRetrieval=" + allowPublicKeyRetrieval
                + "&connectTimeout=10000&socketTimeout=10000";
    }

    public synchronized boolean initialize(Plugin plugin) {
        shutdown();
        try {
            FileConfiguration config = plugin.getConfig();
            mysql = "mysql".equalsIgnoreCase(config.getString("database.type", "h2"));
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setMaximumPoolSize(5);
            hikariConfig.setConnectionTimeout(30000);

            if (mysql) {
                DBConnectionInfo info = getDbConnectionInfo(config);
                hikariConfig.setJdbcUrl(buildMysqlJdbcUrl(config, info));
                hikariConfig.setUsername(info.username());
                hikariConfig.setPassword(info.password());
                hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
                log.info("Using MySQL database at {}:{}/{}", info.host(), info.port(), info.database());
            } else {
                String dataPath = plugin.getDataFolder().getAbsolutePath() + "/data/database";
                hikariConfig.setJdbcUrl("jdbc:h2:" + dataPath + ";MODE=MySQL;AUTO_SERVER=TRUE");
                hikariConfig.setDriverClassName("org.h2.Driver");
                log.info("Using H2 database at {}", dataPath);
            }

            dataSource = new HikariDataSource(hikariConfig);
            jdbi = Jdbi.create(dataSource);
            jdbi.installPlugin(new SqlObjectPlugin());
            initializeMetadataTables();
            available = true;
            isInitialized = true;
            return true;
        } catch (Exception e) {
            log.warn("Database initialization failed: {}", e.getMessage());
            isInitialized = true;
            shutdown();
            return false;
        }
    }

    private void initializeMetadataTables() {
        jdbi.useTransaction(handle -> {
            handle.execute("CREATE TABLE IF NOT EXISTS _table_versions " +
                    "(table_name VARCHAR(64) PRIMARY KEY, version INT NOT NULL)");
            handle.execute("CREATE TABLE IF NOT EXISTS _migration_lock " +
                    "(lock_name VARCHAR(64) PRIMARY KEY, owner VARCHAR(36), lock_timestamp BIGINT NOT NULL DEFAULT 0)");
            handle.createUpdate("INSERT IGNORE INTO _migration_lock (lock_name, owner, lock_timestamp) " +
                            "VALUES (:name, NULL, 0)")
                    .bind("name", MIGRATION_LOCK)
                    .execute();
            handle.execute("CREATE TABLE IF NOT EXISTS _legacy_player_migrations " +
                    "(player_uuid VARCHAR(36) PRIMARY KEY, migrated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        });
    }

    public static HikariDataSource openH2Source(Plugin plugin, int poolSize) {
        String dataPath = plugin.getDataFolder().getAbsolutePath() + "/data/database";
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:h2:" + dataPath + ";MODE=MySQL;AUTO_SERVER=TRUE");
        hikariConfig.setDriverClassName("org.h2.Driver");
        hikariConfig.setMaximumPoolSize(poolSize);
        hikariConfig.setConnectionTimeout(10000);
        hikariConfig.setPoolName("CaveCrawlers-H2-Migration");
        return new HikariDataSource(hikariConfig);
    }

    public record PlayerLease(long fenceToken, long dataRevision) {
    }

    public record WriteOutcome(boolean committed, long currentFenceToken, long currentRevision, boolean owned) {
    }

    private record SessionRow(boolean locked, String serverId, long lockTimestamp,
                              long fenceToken, long dataRevision) {
        boolean ownedBy(String expectedServer, long expectedFence) {
            return locked && expectedServer.equals(serverId) && expectedFence == fenceToken;
        }
    }

    public static final class StaleSessionException extends RuntimeException {
        private final long currentFence;

        public StaleSessionException(UUID uuid, long attemptedFence, long currentFence) {
            super("Stale player session " + uuid + ": attempted fence " + attemptedFence
                    + ", current fence " + currentFence);
            this.currentFence = currentFence;
        }

        public long currentFence() {
            return currentFence;
        }
    }

    private record DBConnectionInfo(String host, int port, String database, String username, String password) {
    }
}
