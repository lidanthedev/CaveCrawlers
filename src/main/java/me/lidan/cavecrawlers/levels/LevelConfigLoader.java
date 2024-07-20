package me.lidan.cavecrawlers.levels;

import me.lidan.cavecrawlers.utils.CustomConfig;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class LevelConfigLoader {
    private static LevelConfigLoader instance;
    private final CustomConfig config;

    private LevelConfigLoader() {
        this.config = new CustomConfig("levels.yml");
        saveDefaultConfig();
    }

    public static LevelConfigLoader getInstance() {
        if (instance == null) {
            instance = new LevelConfigLoader();
        }
        return instance;
    }

    public void saveDefaultConfig() {
        if (!config.getFile().exists()) {
            config.setup();
            config.options().copyDefaults(true);
            config.save();
        }
    }

    public void setLevelInfo(int level, ChatColor color) {
        config.set("levels." + level + ".color", color.name());
        config.save();
    }

    public void setLevelColor(int level, ChatColor color) {
        config.set("levels." + level + ".color", color.name());
        config.save();
    }

    public LevelInfo getLevelInfo(int level) {
        String colorName = config.getString("levels." + level + ".color");
        if (colorName != null) {
            ChatColor color = ChatColor.valueOf(colorName);
            return new LevelInfo(level, color);
        }
        return null;
    }

    public void setPlayerLevelInfo(String playerId, LevelInfo levelInfo) {
        config.set("players." + playerId + ".level", levelInfo.getLevel());
        config.save();
    }

    public LevelInfo getPlayerLevelInfo(String playerId) {
        return getLevelInfo(playerId, config);
    }

    @Nullable
    static LevelInfo getLevelInfo(String playerId, CustomConfig config) {
        ConfigurationSection playerSection = config.getConfigurationSection("players." + playerId);
        if (playerSection != null) {
            int level = playerSection.getInt("level", 1); // Default level is 1
            String colorName = playerSection.getString("color", ChatColor.GRAY.name()); // Default color is GRAY
            ChatColor color = ChatColor.valueOf(colorName);
            return new LevelInfo(level, color);
        }
        return null;
    }

    public String getLevelColor(int level) {
        return config.getString("levels." + level + ".color");
    }

    public String getPlayerLevelColor(String playerId) {
        return config.getString("players." + playerId + ".color");
    }

    public void setPlayerXP(String playerId, int xpAmount) {
        config.set("players." + playerId + ".xp", xpAmount);
        config.save();
    }

    public int getPlayerXP(String playerId) {
        return config.getInt("players." + playerId + ".xp", 0); // Default XP is 0
    }

    public void setPlayerLevel(String playerId, int level) {
        config.set("players." + playerId + ".level", level);
        config.save();
    }

    public int getPlayerLevel(String playerId) {
        return config.getInt("players." + playerId + ".level", 1); // Default level is 1
    }

    public String getPlayerPrefix(String playerId) {
        return config.getString("players." + playerId + ".prefix", "DefaultPrefix");
    }
}
