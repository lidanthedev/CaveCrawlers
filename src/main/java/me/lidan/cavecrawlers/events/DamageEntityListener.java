package me.lidan.cavecrawlers.events;

import me.lidan.cavecrawlers.damage.PlayerDamageCalculation;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

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
        PlayerDamageCalculation calculation = new PlayerDamageCalculation(player);
        double damage = calculation.calculate();
        boolean crit = calculation.isCrit();
        event.setDamage(damage);
        double finalDamage = event.getFinalDamage();

        StringBuilder msg = new StringBuilder();
        if (crit) {
            msg.append(ChatColor.WHITE).append("✧").append(finalDamage).append(ChatColor.WHITE).append("✧");
        } else {
            msg.append(ChatColor.WHITE).append(finalDamage);
        }

        player.sendMessage(msg.toString());


    }

    private void onPlayerDamaged(EntityDamageByEntityEvent event, Player player) {
        double damage = event.getDamage();
        Stats stats = StatsManager.getInstance().getStats(player);
        double defense = stats.get(StatType.DEFENSE).getValue();
        double damageReduction = defense / (defense + 100);

        damage = damage * (1 - damageReduction);
        player.sendMessage("DAMAGED: " + damage + " With RES: " + damageReduction);

        event.setDamage(damage);
    }
}
