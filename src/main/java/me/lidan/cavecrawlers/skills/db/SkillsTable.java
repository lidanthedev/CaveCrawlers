package me.lidan.cavecrawlers.skills.db;

import me.lidan.cavecrawlers.database.SqlTable;
import org.jdbi.v3.core.Handle;

public class SkillsTable extends SqlTable {

    @Override
    public String getTableName() {
        return "skills";
    }

    @Override
    public String getCreateCommand() {
        return """
                CREATE TABLE IF NOT EXISTS skills (
                    player_uuid TEXT NOT NULL,
                    type TEXT NOT NULL,
                    xp REAL NOT NULL,
                    level INTEGER NOT NULL,
                    total_xp REAL NOT NULL,
                    UNIQUE(player_uuid, type)
                )
                """;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public void onCreate(Handle handle) {
    }

    @Override
    public void onUpgrade(Handle handle, int from, int to) {
    }
}
