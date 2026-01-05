package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.entities.EntityManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityDeathListener implements Listener {
    private static EntityManager entityManager = EntityManager.getInstance();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        EntityDamageEvent lastDamageCause = entity.getLastDamageCause();
        if (entity.getKiller() != null && lastDamageCause != null && lastDamageCause.getCause() != EntityDamageEvent.DamageCause.VOID){
            entityManager.onDeath(event);
            event.setDroppedExp(0);
            event.getDrops().clear();
        }
    }
}
