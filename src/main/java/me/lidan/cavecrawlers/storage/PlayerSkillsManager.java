package me.lidan.cavecrawlers.storage;

import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.skills.Skill;
import me.lidan.cavecrawlers.skills.SkillInfo;
import me.lidan.cavecrawlers.skills.Skills;
import me.lidan.cavecrawlers.skills.SkillsManager;
import me.lidan.cavecrawlers.storage.db.Database;
import me.lidan.cavecrawlers.storage.db.SkillRow;
import me.lidan.cavecrawlers.storage.db.SkillsDao;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PlayerSkillsManager {
    private static PlayerSkillsManager instance;

    private final ConcurrentHashMap<UUID, Skills> activeSkills = new ConcurrentHashMap<>();
    /**
     * Holds rows snapshotted at quit time until the async write completes.
     * On rapid reconnect, loadPlayerSync reads from here instead of the DB
     * so the player sees their correct quit-time state immediately.
     */
    private final ConcurrentHashMap<UUID, List<SkillRow>> pendingSaves = new ConcurrentHashMap<>();
    private final CaveCrawlers plugin = CaveCrawlers.getInstance();

    private PlayerSkillsManager() {
    }

    public static PlayerSkillsManager getInstance() {
        if (instance == null) {
            instance = new PlayerSkillsManager();
        }
        return instance;
    }

    public Skills loadPlayerSync(UUID uuid) {
        // If there's an in-flight quit save, use that snapshot so the player
        // sees their correct state rather than stale DB data.
        List<SkillRow> pending = pendingSaves.get(uuid);
        List<SkillRow> rows = (pending != null) ? pending : loadRowsFromDb(uuid);

        List<Skill> skillList = new ArrayList<>();
        for (SkillRow row : rows) {
            SkillInfo skillInfo = SkillsManager.getInstance().getSkillInfo(row.getType());
            if (skillInfo == null) {
                log.warn("Skipping unknown skill type '{}' for player {}", row.getType(), uuid);
                continue;
            }
            // Mirrors Skills.deserialize(): recompute level+xp from totalXp so any
            // XP-requirement config changes are reflected on next login.
            Skill skill = new Skill(skillInfo, 0);
            skill.addXp(row.getTotalXp());
            skill.levelUp(false);
            skillList.add(skill);
        }

        Skills skills = new Skills(skillList);
        skills.setUuid(uuid);
        activeSkills.put(uuid, skills);
        return skills;
    }

    public void savePlayerAsync(UUID uuid) {
        // Snapshot rows immediately on the calling thread so a rapid reconnect
        // cannot replace the cache entry before the async task reads it.
        Skills skills = activeSkills.remove(uuid);
        if (skills == null) {
            return;
        }

        List<SkillRow> rows = buildRows(uuid, skills);
        if (rows.isEmpty()) {
            return;
        }

        pendingSaves.put(uuid, rows);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            writeRows(rows);
            pendingSaves.remove(uuid);
        });
    }

    public void saveAll() {
        for (Map.Entry<UUID, Skills> entry : activeSkills.entrySet()) {
            List<SkillRow> rows = buildRows(entry.getKey(), entry.getValue());
            if (!rows.isEmpty()) {
                writeRows(rows);
            }
        }
        // Flush any quit saves whose async tasks were cancelled (e.g. on shutdown).
        for (List<SkillRow> rows : pendingSaves.values()) {
            writeRows(rows);
        }
        pendingSaves.clear();
    }

    /**
     * Returns cached skills, lazily loading from the database on a cache miss.
     * This maintains the old PlayerDataManager contract and handles reload scenarios
     * where players are already online but the cache was cleared.
     */
    public Skills getSkills(UUID uuid) {
        Skills cached = activeSkills.get(uuid);
        if (cached != null) {
            return cached;
        }
        return loadPlayerSync(uuid);
    }

    public Skills getSkills(Player player) {
        return getSkills(player.getUniqueId());
    }

    public void putSkills(UUID uuid, Skills skills) {
        skills.setUuid(uuid);
        activeSkills.put(uuid, skills);
    }

    public void resetPlayerData(UUID uuid) {
        Skills skills = new Skills();
        skills.setUuid(uuid);
        activeSkills.put(uuid, skills);
        List<SkillRow> rows = buildRows(uuid, skills);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> writeRows(rows));
    }

    public void removeFromCache(UUID uuid) {
        activeSkills.remove(uuid);
    }

    private List<SkillRow> loadRowsFromDb(UUID uuid) {
        return Database.getInstance().getJdbi().withHandle(handle ->
                handle.attach(SkillsDao.class).getSkills(uuid.toString())
        );
    }

    private List<SkillRow> buildRows(UUID uuid, Skills skills) {
        List<SkillRow> rows = new ArrayList<>();
        for (Skill skill : skills) {
            rows.add(new SkillRow(
                    uuid.toString(),
                    skill.getType().getId(),
                    skill.getXp(),
                    skill.getLevel(),
                    skill.getTotalXp()
            ));
        }
        return rows;
    }

    private void writeRows(List<SkillRow> rows) {
        Database.getInstance().getJdbi().useHandle(handle ->
                handle.attach(SkillsDao.class).upsertSkills(rows)
        );
    }
}
