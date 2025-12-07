package me.lidan.cavecrawlers.levels;

import me.lidan.cavecrawlers.stats.ActionBarManager;
import me.lidan.cavecrawlers.utils.CustomConfig;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;


public class LevelConfigManager {
    private static LevelConfigManager instance;
    private final CustomConfig config;
    private final ActionBarManager actionBarManager = ActionBarManager.getInstance();
    private static final int maxXP = 100;

    private LevelConfigManager() {
        this.config = new CustomConfig("levels.yml");
        saveDefaultConfig();
    }

    public static LevelConfigManager getInstance() {
        if (instance == null) {
            instance = new LevelConfigManager();
        }
        return instance;
    }

    public void saveDefaultConfig() {
        if (!config.getFile().exists()) {
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

    public int getPlayerXP(Player player) {
        return getPlayerXP(player.getUniqueId().toString());
    }

    public void givePlayerXP(Player player, int xpAmount) {
        int currentXP = getPlayerXP(player);
        int newXP = currentXP + xpAmount;
        setPlayerXP(player.getUniqueId().toString(), newXP);
        checkLevelUp(player, newXP);
    }

    private void checkLevelUp(Player player, int xp) {
        int currentLevel = getPlayerLevel(player.getUniqueId().toString());
        String message = ChatColor.DARK_AQUA + "Skyblock Level XP " + xp + ChatColor.GRAY + "/" + ChatColor.DARK_AQUA + maxXP;
        ActionBarManager.getInstance().showActionBar(player, message);
        if (xp >= maxXP) {
            int newLevel = currentLevel + 1;
            setPlayerLevel(player.getUniqueId().toString(), newLevel);
            setPlayerXP(player.getUniqueId().toString(), xp - maxXP); // Carry over excess XP
            player.sendMessage("§3▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂",
                    "    §3§lSKYBLOCK LEVEL UP " + "§bLevel " + ChatColor.DARK_GRAY + currentLevel + "§8→§3" + newLevel,
                    "§7",
                    "    §a§lREWARD",
                    "     §8+§a5 §c❤ Health",
                    "§3▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂");
        }
    }
}
