package me.lidan.cavecrawlers.levels;

import me.lidan.cavecrawlers.utils.CustomConfig;
import net.md_5.bungee.api.ChatColor;


public class LevelConfigLoader {
    private static LevelConfigLoader instance;
    private final CustomConfig config;

    private LevelConfigLoader() {
        this.config = new CustomConfig("levels.yml");
        load();
    }

    public static LevelConfigLoader getInstance() {
        if (instance == null) {
            instance = new LevelConfigLoader();
        }
        return instance;
    }

    public void load() {
        saveDefaultConfig();
    }

    public void saveDefaultConfig() {
        if (!config.getFile().exists()) {
            config.setup();
            config.options().copyDefaults(true);
            config.save();
        }
    }
    public int getPlayerXP(String playerId) {
        return config.getInt("players." + playerId + ".xp", 0); // Default XP is 0 if not set
    }

    public void setPlayerXP(String playerId, int xp) {
        config.set("players." + playerId + ".xp", xp);
        config.save();
    }
    public int getPlayerLevel(String playerId) {
        return config.getInt("players." + playerId + ".level", 1); // Default level is 1 if not set
    }

    public void setPlayerLevel(String playerId, int level) {
        config.set("players." + playerId + ".level", level);
        config.save();
    }
    public String getLevelColor(int level) {
        return config.getString("levels." + level + ".color", null); // Return null if not set
    }

    public void setLevelColor(int level, ChatColor color) {
        config.set("levels." + level + ".color", color.name()); // Store color name (as String)
        config.save();
    }

    public void setLevelInfo(int level, ChatColor color) {
        config.set("levels." + level + ".color", color.name()); // Store color name
        config.save();
    }
}
