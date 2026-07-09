package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.entities.EntityManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;

public class EntityRemoveListener implements Listener {

    @EventHandler
    public void onEntityRemove(EntityRemoveEvent event) {
        EntityManager.getInstance().onEntityRemove(event.getEntity());
    }
}
