package me.lidan.cavecrawlers.skills.db;

import me.lidan.cavecrawlers.skills.Skill;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;

public interface SkillsDao {

    @SqlQuery("SELECT type AS typeId, xp, level, total_xp AS totalXp FROM skills WHERE player_uuid = :uuid")
    @RegisterBeanMapper(Skill.class)
    List<Skill> getSkills(@Bind("uuid") String uuid);

    @SqlBatch("""
            INSERT INTO skills (player_uuid, type, xp, level, total_xp)
            VALUES (:uuid, :skill.typeId, :skill.xp, :skill.level, :skill.totalXp)
            ON CONFLICT(player_uuid, type) DO UPDATE SET
                xp = excluded.xp,
                level = excluded.level,
                total_xp = excluded.total_xp
            """)
    void upsertSkills(@Bind("uuid") String uuid, @BindBean("skill") List<Skill> skills);
}
