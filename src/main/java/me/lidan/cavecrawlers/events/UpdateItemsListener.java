package me.lidan.cavecrawlers.events;

import me.lidan.cavecrawlers.items.ItemsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class UpdateItemsListener implements Listener {

    private final ItemsManager itemsManager = ItemsManager.getInstance();

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        itemsManager.updatePlayerInventory(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        itemsManager.updatePlayerInventory(event.getPlayer());
    }
}
