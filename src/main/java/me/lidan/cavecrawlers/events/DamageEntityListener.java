package me.lidan.cavecrawlers.events;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DamageEntityListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player){
            if (event.getEntity() instanceof Mob mob){
                onPlayerDamageMob(event, player, mob);
            }
        } else if (event.getDamager() instanceof Mob mob) {
            if (event.getEntity() instanceof Player player){
                onMobDamagePlayer(event, player, mob);
            }
        }
    }

    private void onMobDamagePlayer(EntityDamageByEntityEvent event, Player player, Mob mob) {

    }

    private void onPlayerDamageMob(EntityDamageByEntityEvent event, Player player, Mob mob){

    }
}
