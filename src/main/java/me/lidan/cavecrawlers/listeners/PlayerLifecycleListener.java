package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.storage.PlayerSkillsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerLifecycleListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        PlayerSkillsManager.getInstance().loadPlayerAsync(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        PlayerSkillsManager.getInstance().savePlayerAsync(event.getPlayer().getUniqueId());
    }
}
