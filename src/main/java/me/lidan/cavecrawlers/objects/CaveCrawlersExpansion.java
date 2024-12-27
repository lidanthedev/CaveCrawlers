package me.lidan.cavecrawlers.objects;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.levels.LevelConfigLoader;
import me.lidan.cavecrawlers.levels.LevelInfo;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.StatsManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.OfflinePlayer;
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
            LevelConfigLoader levelConfigLoader = LevelConfigLoader.getInstance();
            String playerId = player.getUniqueId().toString();
            int level = levelConfigLoader.getPlayerLevel(playerId);
            String colorName = levelConfigLoader.getLevelColor(level);
            ChatColor levelColor = ChatColor.GRAY;
            if (colorName == null) {
                levelConfigLoader.setLevelColor(level, ChatColor.valueOf(levelColor.name()));
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
        }
        return null;
    }
}