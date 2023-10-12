package me.lidan.cavecrawlers.events;

import me.lidan.cavecrawlers.drops.DropsManager;
import me.lidan.cavecrawlers.drops.EntityDrops;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityDeathListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.getKiller() != null){
            Player player = entity.getKiller();
            String name = entity.getName();
            EntityDrops drops = DropsManager.getInstance().getEntityDrops(name);
            if (drops == null) return;
            event.setDroppedExp(0);
            event.getDrops().clear();
            drops.roll(player);
        }
    }
}
