package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class FirstJoinListener implements Listener {
    private final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private final List<String> firstJoinCommands = plugin.getConfig().getStringList("first-join-commands");


    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPlayedBefore()) {
            return;
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
