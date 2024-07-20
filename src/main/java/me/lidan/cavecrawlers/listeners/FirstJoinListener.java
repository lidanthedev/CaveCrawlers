package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.levels.LevelConfigLoader;
import me.lidan.cavecrawlers.levels.LevelInfo;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

public class FirstJoinListener implements Listener {
    private final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private final List<String> firstJoinCommands = plugin.getConfig().getStringList("first-join-commands");

    private final LevelConfigLoader levelConfigLoader;

    public FirstJoinListener(JavaPlugin plugin) {
        this.levelConfigLoader = LevelConfigLoader.getInstance();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerId = player.getUniqueId().toString();

        if (player.hasPlayedBefore()) {
            LevelInfo playerLevelInfo = levelConfigLoader.getPlayerLevelInfo(playerId);
            if (playerLevelInfo != null) {
                return;
            } else {
                player.sendMessage(ChatColor.RED + "Error fetching your level info.");
            }
        } else {
            LevelInfo defaultLevelInfo = new LevelInfo(1, ChatColor.GRAY);
            levelConfigLoader.setPlayerLevelInfo(playerId, defaultLevelInfo); // Set LevelInfo object directly
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
