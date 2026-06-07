package me.lidan.cavecrawlers.storage.db;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

@RegisterBeanMapper(SkillRow.class)
public interface SkillsDao {

    @SqlQuery("SELECT * FROM skills WHERE player_uuid = :uuid")
    List<SkillRow> getSkills(@Bind("uuid") String uuid);

    @SqlQuery("SELECT * FROM skills")
    List<SkillRow> getAllSkills();

    @SqlBatch("""
            INSERT INTO skills (player_uuid, type, xp, level, total_xp)
            VALUES (:playerUuid, :type, :xp, :level, :totalXp)
            ON DUPLICATE KEY UPDATE
              xp = VALUES(xp),
              level = VALUES(level),
              total_xp = VALUES(total_xp)
            """)
    void upsertSkills(@BindBean List<SkillRow> rows);

    @SqlUpdate("DELETE FROM skills WHERE player_uuid = :uuid")
    void deleteSkills(@Bind("uuid") String uuid);
}
