package me.lidan.cavecrawlers.storage.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jspecify.annotations.NonNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class Database {
    private static Database instance;

    private HikariDataSource dataSource;
    private Jdbi jdbi;
    private final CopyOnWriteArrayList<PlayerDataSqlTable> playerDataTables = new CopyOnWriteArrayList<>();
    private volatile boolean available;

    private Database() {
    }

    public static Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    /**
     * Opens a temporary, standalone MySQL data source (caller must close it).
     */
    public static HikariDataSource openMysqlSource(Plugin plugin, int poolSize) {
        FileConfiguration config = plugin.getConfig();
        DBConnectionInfo result = getDbConnectionInfo(config);

        HikariConfig hikariConfig = new HikariConfig();
        boolean ssl = plugin.getConfig().getBoolean("database.ssl", false);
        boolean allowPublicKeyRetrieval = plugin.getConfig().getBoolean("database.allow-public-key-retrieval", true);
        hikariConfig.setJdbcUrl("jdbc:mysql://" + result.host() + ":" + result.port() + "/" + result.database()
                + "?useSSL=" + ssl + "&allowPublicKeyRetrieval=" + allowPublicKeyRetrieval);
        hikariConfig.setUsername(result.username());
        hikariConfig.setPassword(result.password());
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setMaximumPoolSize(poolSize);
        hikariConfig.setConnectionTimeout(10000);
        hikariConfig.setPoolName("CaveCrawlers-MySQL-Migration");
        return new HikariDataSource(hikariConfig);
    }

    public synchronized boolean registerTable(SqlTable table) {
        if (!available || jdbi == null) {
            return false;
        }
        jdbi.useHandle(handle -> {
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

    public void loadPlayerDataTables(UUID playerUuid) {
        for (PlayerDataSqlTable table : playerDataTables) {
            try {
                table.loadForPlayer(playerUuid);
            } catch (Exception e) {
                log.error("Failed to load player data table '{}' for {}", table.getTableName(), playerUuid, e);
            }
        }
    }

    public void savePlayerDataTables(UUID playerUuid) {
        for (PlayerDataSqlTable table : playerDataTables) {
            try {
                table.saveForPlayer(playerUuid);
            } catch (Exception e) {
                log.error("Failed to save player data table '{}' for {}", table.getTableName(), playerUuid, e);
            }
        }
    }

    public synchronized void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        dataSource = null;
        jdbi = null;
        available = false;
    }

    public Jdbi getJdbi() {
        return jdbi;
    }

    public boolean isAvailable() {
        return available;
    }

    private static @NonNull DBConnectionInfo getDbConnectionInfo(FileConfiguration config) {
        String host = config.getString("database.host", "localhost");
        int port = config.getInt("database.port", 3306);
        String database = config.getString("database.database", "cavecrawlers");
        String username = config.getString("database.username", "root");
        String password = config.getString("database.password", "");
        DBConnectionInfo result = new DBConnectionInfo(host, port, database, username, password);
        return result;
    }

    public synchronized boolean initialize(Plugin plugin) {
        shutdown();

        try {
            FileConfiguration config = plugin.getConfig();
            String type = config.getString("database.type", "h2");

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setMaximumPoolSize(5);
            hikariConfig.setConnectionTimeout(30000);

            if ("mysql".equalsIgnoreCase(type)) {
                DBConnectionInfo connectionInfo = getDbConnectionInfo(config);
                boolean ssl = config.getBoolean("database.ssl", false);
                boolean allowPublicKeyRetrieval = config.getBoolean("database.allow-public-key-retrieval", true);
                hikariConfig.setJdbcUrl("jdbc:mysql://" + connectionInfo.host + ":" + connectionInfo.port + "/" + connectionInfo.database
                        + "?useSSL=" + ssl + "&allowPublicKeyRetrieval=" + allowPublicKeyRetrieval);
                hikariConfig.setUsername(connectionInfo.username);
                hikariConfig.setPassword(connectionInfo.password);
                hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
                log.info("Using MySQL database at {}:{}/{}", connectionInfo.host, connectionInfo.port, connectionInfo.database);
            } else {
                String dataPath = plugin.getDataFolder().getAbsolutePath() + "/data/database";
                hikariConfig.setJdbcUrl("jdbc:h2:" + dataPath + ";MODE=MySQL;AUTO_SERVER=TRUE");
                hikariConfig.setDriverClassName("org.h2.Driver");
                log.info("Using H2 database at {}", dataPath);
            }

            dataSource = new HikariDataSource(hikariConfig);
            jdbi = Jdbi.create(dataSource);
            jdbi.installPlugin(new SqlObjectPlugin());

            jdbi.useHandle(handle -> handle.execute(
                    "CREATE TABLE IF NOT EXISTS _table_versions (table_name VARCHAR(64) PRIMARY KEY, version INT NOT NULL)"
            ));
            available = true;
            return true;
        } catch (Exception e) {
            log.warn("Database initialization failed: {}", e.getMessage());
            shutdown();
            return false;
        }
    }

    private record DBConnectionInfo(String host, int port, String database, String username, String password) {
    }

    /**
     * Opens a temporary, standalone H2 data source (caller must close it).
     */
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
}
