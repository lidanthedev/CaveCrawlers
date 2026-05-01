package me.lidan.cavecrawlers.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.skills.Skill;
import me.lidan.cavecrawlers.skills.Skills;
import me.lidan.cavecrawlers.storage.db.Database;
import me.lidan.cavecrawlers.storage.db.SkillRow;
import me.lidan.cavecrawlers.storage.db.SkillsDao;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class YamlMigrationTask extends BukkitRunnable {
    private final Plugin plugin;

    @Override
    public void run() {
        File playersDir = new File(plugin.getDataFolder(), "players");
        if (!playersDir.exists()) {
            return;
        }

        File[] files = playersDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            return;
        }

        log.info("Starting YAML player data migration for {} file(s)...", files.length);
        int succeeded = 0;
        int failed = 0;

        for (File file : files) {
            String filename = file.getName();
            String uuidString = filename.substring(0, filename.length() - ".yml".length());

            UUID uuid;
            try {
                uuid = UUID.fromString(uuidString);
            } catch (IllegalArgumentException e) {
                log.warn("Skipping file with invalid UUID name: {}", filename);
                continue;
            }

            try {
                PlayerData playerData = new PlayerData();
                playerData.loadPlayer(uuid);
                Skills skills = playerData.getSkills();

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

                if (!rows.isEmpty()) {
                    Database.getInstance().getJdbi().useHandle(handle ->
                            handle.attach(SkillsDao.class).upsertSkills(rows)
                    );
                }

                File migrated = new File(file.getParent(), uuidString + ".yml.migrated");
                if (file.renameTo(migrated)) {
                    succeeded++;
                } else {
                    log.warn("Migrated data for {} but could not rename file", uuid);
                    failed++;
                }
            } catch (Exception e) {
                log.error("Failed to migrate player data for {}: {}", uuid, e.getMessage(), e);
                failed++;
            }
        }

        log.info("YAML migration complete: {} succeeded, {} failed", succeeded, failed);
    }
}
