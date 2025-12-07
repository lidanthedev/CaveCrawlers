package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.levels.LevelConfigManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class FirstJoinListener implements Listener {
    private final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private final List<String> firstJoinCommands = plugin.getConfig().getStringList("first-join-commands");

    private final LevelConfigManager levelConfigManager;

    public FirstJoinListener(JavaPlugin plugin) {
        this.levelConfigManager = LevelConfigManager.getInstance();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerId = player.getUniqueId().toString();

        if (player.hasPlayedBefore()) {
            int level = levelConfigManager.getPlayerLevel(playerId);
            if (level > 0) {
                return;
            }
        } else {
            int defaultLevel = 1;
            ChatColor defaultColor = ChatColor.GRAY;
            levelConfigManager.setPlayerLevel(playerId, defaultLevel);
            levelConfigManager.setLevelColor(defaultLevel, defaultColor);
        }
        if (firstJoinCommands.isEmpty()) {
            return;
        }

        ConsoleCommandSender console = Bukkit.getConsoleSender();
        for (String command : firstJoinCommands) {
            Bukkit.dispatchCommand(console, command.replace("%player%", player.getName()));

        }
    }
}
