package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.levels.LevelConfigLoader;
import me.lidan.cavecrawlers.levels.LevelInfo;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
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
            int level = levelConfigLoader.getPlayerLevel(playerId);
            if (level > 0) {
                return;
            }
        } else {
            int defaultLevel = 1;
            ChatColor defaultColor = ChatColor.GRAY;
            levelConfigLoader.setPlayerLevel(playerId, defaultLevel);
            levelConfigLoader.setLevelColor(defaultLevel, defaultColor);
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
