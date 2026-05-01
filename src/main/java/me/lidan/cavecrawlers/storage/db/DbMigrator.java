package me.lidan.cavecrawlers.storage.db;

import com.zaxxer.hikari.HikariDataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import java.util.List;

public class DbMigrator {

    private DbMigrator() {
    }

    /**
     * Wraps an existing data source in a JDBI instance configured with the SQL Object plugin.
     * The caller remains responsible for closing the data source.
     */
    public static Jdbi toJdbi(HikariDataSource dataSource) {
        Jdbi jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new SqlObjectPlugin());
        return jdbi;
    }

    /**
     * Copies all rows from the {@code skills} table in {@code source} into {@code target}.
     * Creates the target schema if it does not exist. Safe to re-run (upsert semantics).
     *
     * @return number of skill records copied
     */
    public static int migrateSkills(Jdbi source, Jdbi target) {
        List<SkillRow> rows = source.withHandle(handle ->
                handle.attach(SkillsDao.class).getAllSkills()
        );

        target.useTransaction(handle -> {
            handle.execute("CREATE TABLE IF NOT EXISTS _table_versions (table_name VARCHAR(64) PRIMARY KEY, version INT NOT NULL)");
            handle.execute(new SkillsTable().getCreateCommand());
            if (!rows.isEmpty()) {
                handle.attach(SkillsDao.class).upsertSkills(rows);
            }
        });

        return rows.size();
    }
}
