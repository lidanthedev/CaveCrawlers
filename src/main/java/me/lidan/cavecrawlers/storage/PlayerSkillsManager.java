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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PlayerSkillsManager {
    private static PlayerSkillsManager instance;

    private final ConcurrentHashMap<UUID, Skills> activeSkills = new ConcurrentHashMap<>();
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
        List<SkillRow> rows = Database.getInstance().getJdbi().withHandle(handle ->
                handle.attach(SkillsDao.class).getSkills(uuid.toString())
        );

        List<Skill> skillList = new ArrayList<>();
        for (SkillRow row : rows) {
            SkillInfo skillInfo = SkillsManager.getInstance().getSkillInfo(row.getType());
            if (skillInfo == null) {
                log.warn("Skipping unknown skill type '{}' for player {}", row.getType(), uuid);
                continue;
            }
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
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> savePlayerSync(uuid));
    }

    private void savePlayerSync(UUID uuid) {
        Skills skills = activeSkills.get(uuid);
        if (skills == null) {
            return;
        }

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

        if (rows.isEmpty()) {
            return;
        }

        Database.getInstance().getJdbi().useHandle(handle ->
                handle.attach(SkillsDao.class).upsertSkills(rows)
        );
    }

    public void saveAll() {
        for (UUID uuid : activeSkills.keySet()) {
            savePlayerSync(uuid);
        }
    }

    public Skills getSkills(UUID uuid) {
        return activeSkills.getOrDefault(uuid, new Skills());
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
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> savePlayerSync(uuid));
    }

    public void removeFromCache(UUID uuid) {
        activeSkills.remove(uuid);
    }
}
