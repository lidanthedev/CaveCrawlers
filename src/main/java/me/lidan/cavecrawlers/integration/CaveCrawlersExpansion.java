package me.lidan.cavecrawlers.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.altar.Altar;
import me.lidan.cavecrawlers.altar.AltarManager;
import me.lidan.cavecrawlers.entities.EntityManager;
import me.lidan.cavecrawlers.levels.LevelConfigManager;
import me.lidan.cavecrawlers.skills.Skill;
import me.lidan.cavecrawlers.skills.Skills;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.storage.PlayerDataManager;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CaveCrawlersExpansion extends PlaceholderExpansion {

    private final StatsManager statsManager;
    private CaveCrawlers plugin;

    public CaveCrawlersExpansion(CaveCrawlers plugin) {
        this.plugin = plugin;
        statsManager = StatsManager.getInstance();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "cavecrawlers";
    }

    @Override
    public @NotNull String getAuthor() {
        return "LidanTheGamer";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String[] args = params.split("_");
        if (args[0].equalsIgnoreCase("stat")) {
            if (args.length >= 2) {
                String statName = params.substring(5); // Remove "stat_" prefix
                StatType statType = StatType.valueOf(statName);
                return String.valueOf(statsManager.getStats(player.getUniqueId()).get(statType).getValue());
            }
        }
        else if (args[0].equalsIgnoreCase("level")) {
            LevelConfigManager levelConfigManager = LevelConfigManager.getInstance();
            String playerId = player.getUniqueId().toString();
            int level = levelConfigManager.getPlayerLevel(playerId);
            String colorName = levelConfigManager.getLevelColor(level);
            ChatColor levelColor = ChatColor.GRAY;
            if (colorName == null) {
                levelConfigManager.setLevelColor(level, ChatColor.valueOf(levelColor.name()));
            } else {
                try {
                    levelColor = ChatColor.valueOf(colorName);
                } catch (IllegalArgumentException e) {
                    if (player instanceof Player) {
                        ((Player) player).sendMessage(ChatColor.RED + "Invalid color in configuration for level " + level);
                    }
                }
            }
            return levelColor + "" + level;
        } else if (args[0].equalsIgnoreCase("altar")) {
            if (args.length < 2) {
                return null;
            }
            AltarManager altarManager = AltarManager.getInstance();
            Altar altar = altarManager.getAltar(args[1]);
            if (altar == null) {
                return "Altar not found";
            }
            if (args.length > 3 && args[2].equalsIgnoreCase("boss")) {
                LivingEntity boss = altar.getSpawnedEntity();
                if (boss == null) {
                    return "false";
                }
                switch (args[3].toLowerCase()) {
                    case "name":
                        return boss.getName();
                    case "health":
                        return StringUtils.valueOf(boss.getHealth());
                    case "maxhealth":
                        return StringUtils.valueOf(boss.getMaxHealth());
                    case "damage":
                        EntityManager entityManager = EntityManager.getInstance();
                        double damage = entityManager.getDamage(player.getUniqueId(), boss);
                        return StringUtils.valueOf(damage);
                    case "alive":
                        return boss.isDead() ? "false" : "true";
                    default:
                        break;
                }
            }
        } else if (args[0].equalsIgnoreCase("skill")) {
            // Expected formats:
            // cavecrawlers_skill_<skillName>_level
            // cavecrawlers_skill_<skillName>_xp
            // cavecrawlers_skill_<skillName>_needed
            // cavecrawlers_skill_<skillName>_progress
            if (args.length < 3) {
                return null;
            }
            if (!(player instanceof Player online)) {
                return null;
            }
            Skills skills = PlayerDataManager.getInstance().getSkills(online);
            String skillName = args[1];
            String field = args[2].toLowerCase();

            for (Skill skill : skills) {
                if (skill.getType().getName().equalsIgnoreCase(skillName)) {
                    switch (field) {
                        case "level":
                            return String.valueOf(skill.getLevel());
                        case "xp":
                            return StringUtils.getNumberFormat(skill.getXp());
                        case "needed":
                            return StringUtils.getShortNumber(skill.getXpToLevel());
                        case "needed_raw":
                            return String.valueOf(skill.getXpToLevel());
                        case "progress":
                            double pct = 0;
                            if (skill.getXpToLevel() > 0) {
                                pct = Math.floor(skill.getXp() / skill.getXpToLevel() * 1000d) / 10d;
                            }
                            return StringUtils.getNumberFormat(Math.min(pct, 100));
                        default:
                            return null;
                    }
                }
            }
            return null;
        }
        return null;
    }
}