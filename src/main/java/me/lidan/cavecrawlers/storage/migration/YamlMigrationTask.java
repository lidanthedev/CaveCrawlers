package me.lidan.cavecrawlers.storage.migration;

import me.lidan.cavecrawlers.skills.Skill;
import me.lidan.cavecrawlers.skills.Skills;
import me.lidan.cavecrawlers.skills.db.SkillsDao;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jdbi.v3.core.Jdbi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class YamlMigrationTask {

    private final Plugin plugin;
    private final Jdbi jdbi;

    public YamlMigrationTask(Plugin plugin, Jdbi jdbi) {
        this.plugin = plugin;
        this.jdbi = jdbi;
    }

    public void run() {
        File playersFolder = new File(plugin.getDataFolder(), "players");
        if (!playersFolder.exists() || !playersFolder.isDirectory()) {
            return;
        }
        File[] files = playersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            return;
        }

        jdbi.useTransaction(handle -> {
            SkillsDao dao = handle.attach(SkillsDao.class);
            for (File file : files) {
                String fileName = file.getName();
                String uuidPart = fileName.substring(0, fileName.length() - 4);
                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidPart);
                } catch (IllegalArgumentException ignored) {
                    continue;
                }

                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                Object rawSkills = config.get("skills");
                if (!(rawSkills instanceof Skills skills)) {
                    continue;
                }

                skills.setUuid(uuid);
                List<Skill> snapshot = new ArrayList<>();
                for (Skill skill : skills) {
                    if (skill.getType() == null || skill.getType().getId() == null) {
                        continue;
                    }
                    Skill clone = new Skill(skill.getType(), skill.getLevel(), skill.getXp(), skill.getXpToLevel(), skill.getTotalXp());
                    clone.setUuid(uuid);
                    snapshot.add(clone);
                }

                if (!snapshot.isEmpty()) {
                    dao.upsertSkills(uuid.toString(), snapshot);
                }

                File migratedFile = new File(playersFolder, fileName + ".migrated");
                if (!file.renameTo(migratedFile)) {
                    plugin.getLogger().warning("Failed to mark migrated file: " + file.getName());
                }
            }
        });
    }
}
