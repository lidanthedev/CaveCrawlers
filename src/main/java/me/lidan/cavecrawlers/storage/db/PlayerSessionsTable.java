package me.lidan.cavecrawlers.storage.db;

import org.jdbi.v3.core.Handle;

public class PlayerSessionsTable extends SqlTable {

    @Override
    public String getTableName() {
        return "player_sessions";
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public String getCreateCommand() {
        return """
                CREATE TABLE IF NOT EXISTS player_sessions (
                  player_uuid    VARCHAR(36)  NOT NULL,
                  is_locked      TINYINT      NOT NULL DEFAULT 0,
                  locking_server VARCHAR(64),
                  lock_timestamp BIGINT       NOT NULL DEFAULT 0,
                  PRIMARY KEY (player_uuid)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """;
    }

    @Override
    public void onCreate(Handle handle) {
        handle.execute(getCreateCommand());
    }

    @Override
    public void onUpgrade(Handle handle, int oldVersion, int newVersion) {
    }
}
