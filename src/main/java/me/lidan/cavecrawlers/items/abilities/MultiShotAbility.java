package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.utils.BukkitUtils;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;


public class MultiShotAbility extends ItemAbility implements Listener {
    public static final String BOW_TAG = "MULTI_SHOT";
    private final int amount;
    private long maxPowerTime = 1000L;
    private double maxPower = 3;
    private int yawDiff = 5;

    public MultiShotAbility(int amount) {
        super("Multi Shot", "Shoots " + amount + " Arrows", 0, 50);
        this.amount = amount;
    }

    public MultiShotAbility(int amount, long maxPowerTime) {
        super("Multi Shot", "Shoots " + amount + " Arrows", 0, 50);
        this.amount = amount;
        this.maxPowerTime = maxPowerTime;
    }

    public MultiShotAbility(int amount, long maxPowerTime, double maxPower, int yawDiff) {
        super("Multi Shot", "Shoots " + amount + " Arrows", 0, 50);
        this.amount = amount;
        this.maxPowerTime = maxPowerTime;
        this.maxPower = maxPower;
        this.yawDiff = yawDiff;
    }

    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        ItemStack item = e.getItem();

        if (!hasAbility(item)) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = e.getPlayer();

        getAbilityCooldown().startCooldown(player.getUniqueId());
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent e) {
        if (e.getEntity().getShooter() instanceof Player player) {
            if (e.getEntityType() == EntityType.ARROW) {
                ItemStack item = player.getEquipment().getItemInMainHand();
                Projectile projectile = e.getEntity();
                if (hasAbility(item)){
                    useAbility(player, projectile);
                }
            }
        }
    }

    private void useAbility(Player player, Projectile projectile) {
        long diff = getAbilityCooldown().getCurrentCooldown(player.getUniqueId());
        if (diff > maxPowerTime){
            diff = maxPowerTime;
        }
        if (diff >= 50) {
            getAbilityCooldown().startCooldown(player.getUniqueId());
            projectile.remove();
            if (diff >= 200) {
                shoot(player, (double) diff);
            }
        }
    }

    public void shoot(Player player, double diff) {
        double multiplier = diff / maxPowerTime * maxPower;
        int yaw = 0;
        for (int i = 0; i < amount; i++) {
            Vector vector = BukkitUtils.getVector(player, yaw, 0, multiplier);
            Arrow arrow = player.launchProjectile(Arrow.class, vector);
            arrow.addScoreboardTag(BOW_TAG);
            if (i % 2 == 0) {
                yaw = Math.abs(yaw) + yawDiff;
            } else {
                yaw = -yaw;
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        if (e.getEntity().getShooter() instanceof Player) {
            if (e.getEntityType() == EntityType.ARROW) {
                if (e.getEntity().getScoreboardTags().contains(BOW_TAG)) {
                    e.getEntity().remove();
                }
            }
        }
    }

    @Override
    protected void useAbility(PlayerEvent playerEvent) {

    }

    @Override
    public void abilityFailedCooldown(Player player) {

    }
}
