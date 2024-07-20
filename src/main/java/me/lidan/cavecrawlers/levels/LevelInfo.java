package me.lidan.cavecrawlers.levels;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import static me.lidan.cavecrawlers.gui.SellMenu.config;

public class LevelInfo {

    private int level;
    private ChatColor color;

    public LevelInfo(int level, ChatColor color) {
        this.level = level;
        this.color = color;
    }

    public int getLevel() {
        return level;
    }

    public ChatColor getColor() {
        return color;
    }

    public ConfigurationSection serialize() {
        ConfigurationSection section = new YamlConfiguration();
        section.set("level", level);
        section.set("color", color.name());
        return section;
    }

    public static LevelInfo deserialize(ConfigurationSection section) {
        int level = section.getInt("level");
        ChatColor color = ChatColor.valueOf(section.getString("color"));
        return new LevelInfo(level, color);
    }
    public static LevelInfo getPlayerLevelInfo(String playerId) {
        // Replace this with your actual implementation to fetch player level info from config
        ConfigurationSection playerSection = config.getConfigurationSection("players." + playerId);
        if (playerSection != null) {
            int level = playerSection.getInt("level", 1); // Default level is 1
            String colorName = playerSection.getString("color", ChatColor.GRAY.name()); // Default color is GRAY
            ChatColor color = ChatColor.valueOf(colorName);
            return new LevelInfo(level, color);
        }
        return null; // Handle if player's level info is not found
    }
}