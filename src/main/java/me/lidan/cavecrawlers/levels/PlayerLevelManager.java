package me.lidan.cavecrawlers.levels;

import me.lidan.cavecrawlers.stats.ActionBarManager;
import me.lidan.cavecrawlers.stats.Stats;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class PlayerLevelManager {

    private final LevelConfigLoader levelConfigLoader;
    private final Stats stats;
    private final ActionBarManager actionBarManager;

    public PlayerLevelManager(LevelConfigLoader levelConfigLoader, Stats stats, ActionBarManager actionBarManager) {
        this.levelConfigLoader = levelConfigLoader;
        this.stats = stats;
        this.actionBarManager = actionBarManager;
    }

    public int getPlayerXP(Player player) {
        return levelConfigLoader.getPlayerXP(player.getUniqueId().toString());
    }
    private final int xpPerEnderman = 5; // Adjust XP per Enderman as needed

    public void givePlayerXP(Player player, int xpAmount) {
        int currentXP = getPlayerXP(player);
        int newXP = currentXP + xpAmount;
        levelConfigLoader.setPlayerXP(player.getUniqueId().toString(), newXP);

        // Check for level up
        checkLevelUp(player, newXP);
    }

    private void checkLevelUp(Player player, int xp) {
        int currentLevel = levelConfigLoader.getPlayerLevel(player.getUniqueId().toString());
        int maxXP = 100; // Max XP per level
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
    public void giveXPForKillingEndermen(Player player, int xpPerEnderman) {
        givePlayerXP(player, xpPerEnderman);
    }
}