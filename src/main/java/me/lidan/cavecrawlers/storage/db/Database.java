package me.lidan.cavecrawlers.storage.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

@Slf4j
public class Database {
    private static Database instance;

    private HikariDataSource dataSource;
    @Getter
    private Jdbi jdbi;

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
        hikariConfig.setJdbcUrl("jdbc:mysql://" + result.host() + ":" + result.port() + "/" + result.database() + "?useSSL=false&allowPublicKeyRetrieval=true");
        hikariConfig.setUsername(result.username());
        hikariConfig.setPassword(result.password());
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setMaximumPoolSize(poolSize);
        hikariConfig.setConnectionTimeout(10000);
        hikariConfig.setPoolName("CaveCrawlers-MySQL-Migration");
        return new HikariDataSource(hikariConfig);
    }

    public void registerTable(SqlTable table) {
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
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
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

    public void initialize(Plugin plugin) {
        FileConfiguration config = plugin.getConfig();
        String type = config.getString("database.type", "h2");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setMaximumPoolSize(5);
        hikariConfig.setConnectionTimeout(30000);

        if ("mysql".equalsIgnoreCase(type)) {
            DBConnectionInfo connectionInfo = getDbConnectionInfo(config);
            hikariConfig.setJdbcUrl("jdbc:mysql://" + connectionInfo.host + ":" + connectionInfo.port + "/" + connectionInfo.database + "?useSSL=false&allowPublicKeyRetrieval=true");
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
