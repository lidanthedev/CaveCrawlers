package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.damage.DamageCalculation;
import me.lidan.cavecrawlers.damage.DamageManager;
import me.lidan.cavecrawlers.entities.EntityManager;
import me.lidan.cavecrawlers.stats.ActionBarManager;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.utils.Holograms;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DamageEntityListener implements Listener {

    private static final boolean PROJECTILE_DAMAGE_FIX = true;
    private static final Logger log = LoggerFactory.getLogger(DamageEntityListener.class);
    private static final EntityManager entityManager = EntityManager.getInstance();
    private static final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private static final double VOID_DAMAGE = 1000000000;
    private static double serverDamageMultiplier = plugin.getConfig().getDouble("server-damage-multiplier", 1);

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
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

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        double damage = event.getDamage();
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID){
            damage = VOID_DAMAGE;
        }

        event.setDamage(damage);
    }

    private void onPlayerDamageMobProjectile(EntityDamageByEntityEvent event, Projectile projectile, Player player, Mob mob){
        if (event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) {
            onPlayerDamageMob(event, player, mob);
            return;
        }
        player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        PersistentDataContainer container = projectile.getPersistentDataContainer();
        Double calculated = container.get(new NamespacedKey(plugin, "calculated"), PersistentDataType.DOUBLE);
        Boolean crit = container.get(new NamespacedKey(plugin, "crit"), PersistentDataType.BOOLEAN);
        if (calculated == null || crit == null){
            onPlayerDamageMob(event, player, mob);
            return;
        }
        calculated *= serverDamageMultiplier;
        damageMobAfterCalculation(event, player, mob, calculated, crit);
    }

    private void onPlayerDamageMob(EntityDamageByEntityEvent event, Player player, Mob mob) {
        mob.setMaximumNoDamageTicks(0);
        DamageManager damageManager = DamageManager.getInstance();
        if (event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE && !damageManager.canAttack(player, mob)){
            event.setCancelled(true);
            return;
        }
        ItemStack hand = player.getEquipment().getItemInMainHand();
        if (hand.getType() == Material.BOW && event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE){
            event.setCancelled(true);
            PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.LEFT_CLICK_AIR, hand, null, null, EquipmentSlot.HAND);
            Bukkit.getPluginManager().callEvent(interactEvent);
            return;
        }

        DamageCalculation calculation = damageManager.getDamageCalculation(player);
        double damage = calculation.calculate();
        boolean crit = calculation.isCrit();
        damage *= serverDamageMultiplier;
        damageMobAfterCalculation(event, player, mob, damage, crit);
    }

    private static void damageMobAfterCalculation(EntityDamageByEntityEvent event, Player player, Mob mob, double damage, boolean crit) {
        event.setDamage(damage);
        int finalDamage = (int) event.getFinalDamage();
        entityManager.addDamage(player.getUniqueId(), mob, finalDamage);
        Holograms.showDamageHologram(mob, finalDamage, crit);
    }

    private void onPlayerDamaged(EntityDamageByEntityEvent event, Player player) {
        double damage = event.getDamage();

        if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE && PROJECTILE_DAMAGE_FIX){
            Entity attacker = event.getDamager();
            if (attacker instanceof Projectile projectile){
                if (projectile.getShooter() instanceof Mob mob) {
                    damage = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
                }
            }
        }

        Stats stats = StatsManager.getInstance().getStats(player);
        double defense = stats.get(StatType.DEFENSE).getValue();
        double damageReduction = defense / (defense + 100);

        damage = damage * (1 - damageReduction);

        event.setDamage(damage);

        Bukkit.getScheduler().runTaskLater(plugin, bukkitTask -> {
            ActionBarManager.getInstance().actionBar(player);
        }, 1L);
    }
}
