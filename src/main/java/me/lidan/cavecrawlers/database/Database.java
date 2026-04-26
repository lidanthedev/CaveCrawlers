package me.lidan.cavecrawlers.database;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import java.io.File;

public class Database {

    private final Jdbi jdbi;

    public Database(File pluginDataFolder) {
        File dataDir = new File(pluginDataFolder, "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        File dbFile = new File(dataDir, "database.db");
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath() + "?journal_mode=WAL";
        this.jdbi = Jdbi.create(url);
        this.jdbi.installPlugin(new SqlObjectPlugin());
    }

    public Jdbi getJdbi() {
        return jdbi;
    }

    public void registerTable(SqlTable table) {
        jdbi.useTransaction(handle -> {
            handle.execute("CREATE TABLE IF NOT EXISTS _table_versions (table_name TEXT PRIMARY KEY, version INTEGER NOT NULL)");
            Integer currentVersion = handle.createQuery("SELECT version FROM _table_versions WHERE table_name = :table_name")
                    .bind("table_name", table.getTableName())
                    .mapTo(Integer.class)
                    .findOne()
                    .orElse(0);

            if (currentVersion == 0) {
                handle.execute(table.getCreateCommand());
                table.onCreate(handle);
                handle.createUpdate("INSERT INTO _table_versions (table_name, version) VALUES (:table_name, :version)")
                        .bind("table_name", table.getTableName())
                        .bind("version", table.getVersion())
                        .execute();
                return;
            }

            if (currentVersion < table.getVersion()) {
                table.onUpgrade(handle, currentVersion, table.getVersion());
                handle.createUpdate("UPDATE _table_versions SET version = :version WHERE table_name = :table_name")
                        .bind("table_name", table.getTableName())
                        .bind("version", table.getVersion())
                        .execute();
            }
        });
    }
}
