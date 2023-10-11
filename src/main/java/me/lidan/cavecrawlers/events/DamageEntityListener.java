package me.lidan.cavecrawlers.events;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.damage.DamageCalculation;
import me.lidan.cavecrawlers.damage.DamageManager;
import me.lidan.cavecrawlers.stats.*;
import me.lidan.cavecrawlers.utils.Holograms;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class DamageEntityListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (event.getEntity() instanceof Mob mob) {
                onPlayerDamageMob(event, player, mob);
            }
        } else if (event.getEntity() instanceof Player player) {
            onPlayerDamaged(event, player);
        } else if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player player) {
                if (event.getEntity() instanceof Mob mob) {
                    onPlayerDamageMobProjectile(event, projectile, player, mob);
                }
            }
        }
    }

    private void onPlayerDamageMobProjectile(EntityDamageByEntityEvent event, Projectile projectile, Player player, Mob mob){
        if (event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) {
            onPlayerDamageMob(event, player, mob);
            return;
        }
        PersistentDataContainer container = projectile.getPersistentDataContainer();
        Double calculated = container.get(new NamespacedKey(CaveCrawlers.getInstance(), "calculated"), PersistentDataType.DOUBLE);
        Boolean crit = container.get(new NamespacedKey(CaveCrawlers.getInstance(), "crit"), PersistentDataType.BOOLEAN);
        if (calculated == null || crit == null){
            onPlayerDamageMob(event, player, mob);
            return;
        }
        damageMobAfterCalculation(event, mob, calculated, crit);
    }

    private void onPlayerDamageMob(EntityDamageByEntityEvent event, Player player, Mob mob) {
        mob.setMaximumNoDamageTicks(0);
        DamageManager damageManager = DamageManager.getInstance();
        if (event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE && !damageManager.canAttack(player, mob)){
            event.setCancelled(true);
            return;
        }

        DamageCalculation calculation = damageManager.getDamageCalculation(player);
        double damage = calculation.calculate();
        boolean crit = calculation.isCrit();
        damageMobAfterCalculation(event, mob, damage, crit);
    }

    private static void damageMobAfterCalculation(EntityDamageByEntityEvent event, Mob mob, double damage, boolean crit) {
        event.setDamage(damage);
        int finalDamage = (int) event.getFinalDamage();
        Holograms.showDamageHologram(mob, finalDamage, crit);
    }

    private void onPlayerDamaged(EntityDamageByEntityEvent event, Player player) {
        double damage = event.getDamage();
        Stats stats = StatsManager.getInstance().getStats(player);
        double defense = stats.get(StatType.DEFENSE).getValue();
        double damageReduction = defense / (defense + 100);

        damage = damage * (1 - damageReduction);

        event.setDamage(damage);

        Bukkit.getScheduler().runTaskLater(CaveCrawlers.getInstance(), bukkitTask -> {
            ActionBarManager.getInstance().actionBar(player);
        }, 1L);
    }
}
