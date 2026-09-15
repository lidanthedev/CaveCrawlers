package me.lidan.cavecrawlers.storage.db;

import org.jdbi.v3.core.Handle;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PlayerSessionsTable extends SqlTable {

    @Override
    public String getTableName() {
        return "player_sessions";
    }

    @Override
    public int getVersion() {
        return 2;
    }

    @Override
    public String getCreateCommand() {
        return """
                CREATE TABLE IF NOT EXISTS player_sessions (
                  player_uuid    VARCHAR(36)  NOT NULL,
                  is_locked      TINYINT      NOT NULL DEFAULT 0,
                  locking_server VARCHAR(64),
                  lock_timestamp BIGINT       NOT NULL DEFAULT 0,
                  fence_token    BIGINT       NOT NULL DEFAULT 0,
                  data_revision  BIGINT       NOT NULL DEFAULT 0,
                  PRIMARY KEY (player_uuid)
                )
                """;
    }

    @Override
    public void onCreate(Handle handle) {
        handle.execute(getCreateCommand());
    }

    @Override
    public void onUpgrade(Handle handle, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            addColumnIfMissing(handle, "fence_token", "BIGINT NOT NULL DEFAULT 0");
            addColumnIfMissing(handle, "data_revision", "BIGINT NOT NULL DEFAULT 0");
        }
    }

    private void addColumnIfMissing(Handle handle, String column, String definition) {
        try {
            if (hasColumn(handle, column) || hasColumn(handle, column.toUpperCase())) {
                return;
            }
            handle.execute("ALTER TABLE player_sessions ADD COLUMN " + column + " " + definition);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not inspect player_sessions schema", e);
        }
    }

    private boolean hasColumn(Handle handle, String column) throws SQLException {
        try (ResultSet columns = handle.getConnection().getMetaData().getColumns(
                handle.getConnection().getCatalog(), null, "player_sessions", column)) {
            if (columns.next()) {
                return true;
            }
        }
        try (ResultSet columns = handle.getConnection().getMetaData().getColumns(
                handle.getConnection().getCatalog(), null, "PLAYER_SESSIONS", column)) {
            return columns.next();
        }
    }
}
