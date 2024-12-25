package me.lidan.cavecrawlers.levels;

import me.lidan.cavecrawlers.stats.ActionBarManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PlayerLevelManager {

    private final LevelConfigLoader levelConfigLoader;
    private final ActionBarManager actionBarManager;
    private static final int maxXP = 100;

    public PlayerLevelManager(LevelConfigLoader levelConfigLoader) {
        this.levelConfigLoader = levelConfigLoader;
        this.actionBarManager = ActionBarManager.getInstance();
    }

    public int getPlayerXP(Player player) {
        return levelConfigLoader.getPlayerXP(player.getUniqueId().toString());
    }

    public void givePlayerXP(Player player, int xpAmount) {
        int currentXP = getPlayerXP(player);
        int newXP = currentXP + xpAmount;
        levelConfigLoader.setPlayerXP(player.getUniqueId().toString(), newXP);
        checkLevelUp(player, newXP);
    }

    private void checkLevelUp(Player player, int xp) {
        int currentLevel = levelConfigLoader.getPlayerLevel(player.getUniqueId().toString());
        actionBarManager.sendActionBar(player, ChatColor.DARK_AQUA + "Skyblock Level XP " + xp + ChatColor.GRAY + "/" + ChatColor.DARK_AQUA + maxXP);
        if (xp >= maxXP) {
            int newLevel = currentLevel + 1;
            levelConfigLoader.setPlayerLevel(player.getUniqueId().toString(), newLevel);
            levelConfigLoader.setPlayerXP(player.getUniqueId().toString(), xp - maxXP); // Carry over excess XP
            player.sendMessage("§3▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂",
                    "    §3§lSKYBLOCK LEVEL UP " + "§bLevel " + ChatColor.DARK_GRAY + currentLevel + "§8→§3" + newLevel,
                    "§7",
                    "    §a§lREWARD",
                    "     §8+§a5 §c❤ Health",
                    "§3▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂");
        }
    }
}