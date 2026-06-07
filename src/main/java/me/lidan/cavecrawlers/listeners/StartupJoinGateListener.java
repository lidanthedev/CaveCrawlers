package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.CaveCrawlers;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class StartupJoinGateListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        CaveCrawlers plugin = CaveCrawlers.getInstance();
        if (plugin.isLoginAllowed()) {
            return;
        }

        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                Component.text("Server is still starting. Please try again in a moment."));
    }
}
