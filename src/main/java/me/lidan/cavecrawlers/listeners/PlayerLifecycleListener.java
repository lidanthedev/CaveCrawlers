package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.skills.SkillsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerLifecycleListener implements Listener {

    private final SkillsManager skillsManager;

    public PlayerLifecycleListener(SkillsManager skillsManager) {
        this.skillsManager = skillsManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        skillsManager.loadPlayerSync(event.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        skillsManager.savePlayerAsync(event.getPlayer().getUniqueId());
    }
}
