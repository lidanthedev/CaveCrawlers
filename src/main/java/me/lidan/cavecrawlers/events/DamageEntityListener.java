package me.lidan.cavecrawlers.events;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.damage.DamageCalculation;
import me.lidan.cavecrawlers.damage.DamageManager;
import me.lidan.cavecrawlers.damage.PlayerDamageCalculation;
import me.lidan.cavecrawlers.stats.*;
import me.lidan.cavecrawlers.utils.Cooldown;
import me.lidan.cavecrawlers.utils.Holograms;
import me.lidan.cavecrawlers.utils.RandomUtils;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
                    onPlayerDamageMob(event, player, mob);
                }
            }
        }
    }

    private void onPlayerDamageMob(EntityDamageByEntityEvent event, Player player, Mob mob) {
        mob.setMaximumNoDamageTicks(0);
        DamageManager damageManager = DamageManager.getInstance();
        if (!damageManager.canAttack(player, mob)){
            event.setCancelled(true);
            return;
        }

        DamageCalculation calculation = damageManager.getDamageCalculation(player);
        double damage = calculation.calculate();
        boolean crit = calculation.isCrit();
        event.setDamage(damage);
        int finalDamage = (int) event.getFinalDamage();

        StringBuilder msg = new StringBuilder();
        String formattedDamage;
        if (crit) {
            msg.append("✧").append(finalDamage).append("✧");
            formattedDamage = StringUtils.rainbowText(msg.toString());
        } else {
            msg.append(ChatColor.GRAY).append(finalDamage);
            formattedDamage = msg.toString();
        }

        Location hologram = mob.getLocation();
        double random = RandomUtils.randomDouble(1, 1.5);
        hologram.add(mob.getLocation().getDirection().multiply(random));
        hologram.setY(mob.getLocation().getY() + random);
        hologram.subtract(0, 2, 0);
        Holograms.spawnTempArmorStand(hologram, formattedDamage, 10);

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
