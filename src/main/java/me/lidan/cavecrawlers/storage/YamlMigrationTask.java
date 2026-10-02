package me.lidan.cavecrawlers.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.skills.Skill;
import me.lidan.cavecrawlers.skills.Skills;
import me.lidan.cavecrawlers.storage.db.Database;
import me.lidan.cavecrawlers.storage.db.SkillRow;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class YamlMigrationTask extends BukkitRunnable {
    private final CaveCrawlers plugin;

    @Override
    public void run() {
        File playersDir = new File(plugin.getDataFolder(), "players");
        if (!playersDir.exists()) {
            plugin.markLegacyYamlMigrationComplete();
            return;
        }

        File[] files = playersDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.markLegacyYamlMigrationComplete();
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

                boolean imported = Database.getInstance().importLegacySkills(uuid, rows);

                File migrated = new File(file.getParent(), uuidString + ".yml.migrated");
                if (file.renameTo(migrated)) {
                    succeeded++;
                    if (!imported) {
                        log.info("Skipped legacy YAML for {} because authoritative DB data or a migration marker exists", uuid);
                    }
                } else {
                    log.warn("Migrated data for {} but could not rename file", uuid);
                    failed++;
                }
            } catch (Exception e) {
                log.error("Failed to migrate player data for {}: {}", uuid, e.getMessage(), e);
                failed++;
            }
        }
        if (failed == 0) {
            plugin.markLegacyYamlMigrationComplete();
        } else if (plugin.isEnabled()) {
            // Keep the login/load gate closed. Successful files were renamed; only failures retry.
            new YamlMigrationTask(plugin).runTaskLaterAsynchronously(plugin, 20L * 10L);
        }

        log.info("YAML migration complete: {} succeeded, {} failed", succeeded, failed);
    }
}
