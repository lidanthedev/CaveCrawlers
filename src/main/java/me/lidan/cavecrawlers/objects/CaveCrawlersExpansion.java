package me.lidan.cavecrawlers.objects;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.altar.Altar;
import me.lidan.cavecrawlers.altar.AltarManager;
import me.lidan.cavecrawlers.entities.EntityManager;
import me.lidan.cavecrawlers.levels.LevelConfigManager;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaveCrawlersExpansion extends PlaceholderExpansion {

    private static final Logger log = LoggerFactory.getLogger(CaveCrawlersExpansion.class);
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
            plugin.getLogger().info("Placeholder Stat");
            if (args.length == 2) {
                plugin.getLogger().info("Getting stat " + args[1] + " for " + player.getName());
                StatType statType = StatType.valueOf(args[1]);
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
                return null;
            }
            if (args.length > 3 && args[2].equalsIgnoreCase("boss")) {
                LivingEntity boss = altar.getSpawnedEntity();
                if (boss == null) {
                    return null;
                }
                if (args[3].equalsIgnoreCase("name")) {
                    return boss.getName();
                } else if (args[3].equalsIgnoreCase("health")) {
                    return StringUtils.valueOf(boss.getHealth());
                } else if (args[3].equalsIgnoreCase("maxhealth")) {
                    return StringUtils.valueOf(boss.getMaxHealth());
                } else if (args[3].equalsIgnoreCase("damage")) {
                    EntityManager entityManager = EntityManager.getInstance();
                    double damage = entityManager.getDamage(player.getUniqueId(), boss);
                    return StringUtils.valueOf(damage);
                } else if (args[3].equalsIgnoreCase("alive")) {
                    return boss.isDead() ? "false" : "true";
                }
            }
        }
        return null;
    }
}