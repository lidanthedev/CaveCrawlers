package me.lidan.cavecrawlers.levels;

import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.utils.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class  LevelConfigLoader {

    private static LevelConfigLoader instance;
    private static final Logger log = LoggerFactory.getLogger(LevelConfigLoader.class);
    private final File configFile;
    private final FileConfiguration config;
    protected Map<String, String> placeholders = new HashMap<>();

    private LevelConfigLoader(JavaPlugin plugin) {
        this.configFile = new File(plugin.getDataFolder(), "levels.yml");
        this.config = YamlConfiguration.loadConfiguration(configFile);
        Level(null);

        saveDefaultConfig();

    }
    public void Level(Player player){
        level(0, player);
    }
    public static LevelConfigLoader getInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new LevelConfigLoader(plugin);
        }
        return instance;
    }

    public void saveDefaultConfig() {
        if (!configFile.exists()) {
            config.options().copyDefaults(true);
            saveConfig();
        }
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void level(int level, Player player) {
        placeholders.clear();
        placeholders.putAll(Map.of("level", StringUtils.getNumberFormat(level)));
        log.info("ERROR level plceholder");

    }
    public void setLevelInfo(int level, ChatColor color) {
        config.set("levels." + level + ".color", color.name());
        saveConfig();
    }
    public void setLevelColor(String playerId, int level, ChatColor color) {
        config.set("levels." + level + ".color", color.name());
        saveConfig();
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
        config.set("players." + playerId, levelInfo.getLevel());
        saveConfig();
    }

    public LevelInfo getPlayerLevelInfo(String playerId) {
        ConfigurationSection playerSection = config.getConfigurationSection("players." + playerId);
        if (playerSection != null) {
            int level = playerSection.getInt("level", 1); // Default level is 1
            String colorName = playerSection.getString("color", ChatColor.GRAY.name()); // Default color is GRAY
            ChatColor color = ChatColor.valueOf(colorName);
            return new LevelInfo(level, color);
        }
        return null; // Handle if player's level info is not found
    }
    public String getLevelColor(int level) {
        return config.getString("levels." + level + ".color");
    }
    public String getPlayerLevelColor(String playerId) {
        return config.getString("levels." + playerId + ".color");
    }
    public void setPlayerXP(String playerId, int xpAmount) {
        config.set("players." + playerId + ".xp", xpAmount);
        saveConfig();
    }

    public int getPlayerXP(String playerId) {
        return config.getInt("players." + playerId + ".xp", 0); // Default XP is 0
    }

    public void setPlayerLevel(String playerId, int level) {
        config.set("players." + playerId + ".level", level);
        saveConfig();
    }

    public int getPlayerLevel(String playerId) {
        return config.getInt("players." + playerId + ".level", 1); // Default level is 1
    }
    public String getPlayerPrefix(String playerId) {
        return config.getString("players." + playerId + ".prefix", "DefaultPrefix");
    }
}