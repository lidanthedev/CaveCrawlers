package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.storage.PlayerSkillsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerLifecycleListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        UUID uuid = event.getUniqueId();
        PlayerSkillsManager.getInstance().loadPlayerSync(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Blocking save: guarantees the row is in the DB before BungeeCord/Velocity
        // can route the player to another backend and trigger a load there.
        PlayerSkillsManager.getInstance().savePlayerNow(event.getPlayer().getUniqueId());
    }
}
