package me.lidan.cavecrawlers.integration;

import fr.robotv2.placeholderannotationlib.annotations.Placeholder;
import fr.robotv2.placeholderannotationlib.annotations.RequireOnlinePlayer;
import fr.robotv2.placeholderannotationlib.api.BasePlaceholderExpansion;
import fr.robotv2.placeholderannotationlib.api.PlaceholderActor;
import fr.robotv2.placeholderannotationlib.api.PlaceholderAnnotationProcessor;
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

public class CaveCrawlersExpansion extends BasePlaceholderExpansion {

    private final StatsManager statsManager;

    public CaveCrawlersExpansion(PlaceholderAnnotationProcessor processor) {
        super(processor);
        this.statsManager = StatsManager.getInstance();
    }

    /**
     * Handles: %cavecrawlers_stat_<StatType>%
     * Uses varargs (String...) to capture split words like "MAGIC", "FIND"
     * and joins them back together.
     */
    @Placeholder("stat")
    public String getStat(@NotNull PlaceholderActor actor, @NotNull String... rawStatNameParts) {
        if (rawStatNameParts.length == 0) return null;

        // Rejoin parts: ["MAGIC", "FIND"] -> "MAGIC_FIND"
        String statName = String.join("_", rawStatNameParts);

        try {
            StatType statType = StatType.valueOf(statName.toUpperCase());
            // Safe unwrap of OfflinePlayer
            OfflinePlayer p = actor.getPlayer();
            if (p == null) return null;

            return String.valueOf(statsManager.getStats(p.getUniqueId()).get(statType).getValue());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Handles: %cavecrawlers_level%
     */
    @Placeholder("level")
    public String getLevel(@NotNull PlaceholderActor actor) {
        OfflinePlayer player = actor.getPlayer();
        if (player == null) return null;

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
                if (player.isOnline()) {
                    ((Player) player).sendMessage(ChatColor.RED + "Invalid color in configuration for level " + level);
                }
            }
        }
        return levelColor + "" + level;
    }

    /**
     * Handles: %cavecrawlers_altar_<altarName>_boss_<field>%
     */
    @Placeholder("altar")
    public String getAltar(@NotNull PlaceholderActor actor, String altarName, String subType, String field) {
        // For now altar only supports "boss" subtype
        if (!subType.equalsIgnoreCase("boss")) {
            return null;
        }

        AltarManager altarManager = AltarManager.getInstance();
        Altar altar = altarManager.getAltar(altarName);

        if (altar == null) {
            return "Altar not found";
        }

        LivingEntity boss = altar.getSpawnedEntity();
        if (boss == null) {
            return "false";
        }

        switch (field.toLowerCase()) {
            case "name":
                return boss.getName();
            case "health":
                return StringUtils.valueOf(boss.getHealth());
            case "maxhealth":
                return StringUtils.valueOf(boss.getMaxHealth());
            case "damage":
                OfflinePlayer p = actor.getOnlinePlayer();
                if (p != null) {
                    EntityManager entityManager = EntityManager.getInstance();
                    double damage = entityManager.getDamage(p.getUniqueId(), boss);
                    return StringUtils.valueOf(damage);
                }
                return "0";
            case "alive":
                return boss.isDead() ? "false" : "true";
            default:
                return null;
        }
    }

    /**
     * Handles: %cavecrawlers_skill_<skillName>_<field>%
     * Fields: level, xp, needed, needed_raw, progress
     */
    @Placeholder("skill")
    @RequireOnlinePlayer
    public String getSkill(@NotNull PlaceholderActor actor, String skillName, String field) {
        Player online = actor.getOnlinePlayer();
        if (online == null) {
            return null;
        }

        Skills skills = PlayerDataManager.getInstance().getSkills(online);
        field = field.toLowerCase();

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
}