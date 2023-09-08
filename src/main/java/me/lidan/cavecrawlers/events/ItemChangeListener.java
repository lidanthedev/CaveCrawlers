package me.lidan.cavecrawlers.events;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.stats.StatsManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.scheduler.BukkitScheduler;

public class ItemChangeListener implements Listener {

    private final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private final BukkitScheduler scheduler = plugin.getServer().getScheduler();

    @EventHandler(ignoreCancelled = true)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {

        Player player = event.getPlayer();

        scheduler.runTaskLater(plugin, bukkitTask -> {
            StatsManager.getInstance().calculateStats(player);
        }, 1);
    }
}
