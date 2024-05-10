package me.lidan.cavecrawlers.listeners;

import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

public class EntityChangeBlockListener implements Listener {

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        // Check if the entity is a falling block
        if (event.getEntity() instanceof FallingBlock) {
            // Cancel the event to prevent the block from forming on the ground
            event.setCancelled(true);
        }
    }
}
