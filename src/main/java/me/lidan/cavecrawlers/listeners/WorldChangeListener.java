package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class WorldChangeListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if(player.getGameMode() == GameMode.CREATIVE){
            if(player.hasPermission("cavecrawlers.worldchange.Creative")){
                Bukkit.getScheduler().runTaskLater(CaveCrawlers.getInstance(),() -> {
                    player.setGameMode(GameMode.CREATIVE);
                }, 2L);
            }
        }
    }
}
