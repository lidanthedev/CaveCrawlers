package me.lidan.cavecrawlers.storage.db;

import org.jdbi.v3.core.Handle;

public class SkillsTable extends SqlTable {

    @Override
    public String getTableName() {
        return "skills";
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public String getCreateCommand() {
        return """
                CREATE TABLE IF NOT EXISTS skills (
                  player_uuid VARCHAR(36) NOT NULL,
                  type        VARCHAR(64) NOT NULL,
                  xp          DOUBLE      NOT NULL DEFAULT 0,
                  level       INT         NOT NULL DEFAULT 0,
                  total_xp    DOUBLE      NOT NULL DEFAULT 0,
                  UNIQUE KEY uq_player_skill (player_uuid, type)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """;
    }

    @Override
    public void onCreate(Handle handle) {
        handle.execute(getCreateCommand());
    }

    @Override
    public void onUpgrade(Handle handle, int oldVersion, int newVersion) {
        // no upgrades yet for version 1
    }
}
