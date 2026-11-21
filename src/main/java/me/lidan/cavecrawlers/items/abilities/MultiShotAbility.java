package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.damage.DamageCalculation;
import me.lidan.cavecrawlers.damage.DamageManager;
import me.lidan.cavecrawlers.damage.PlayerDamageCalculation;
import me.lidan.cavecrawlers.utils.BukkitUtils;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.Vector;


public class MultiShotAbility extends ItemAbility implements Listener {
    public static final String BOW_TAG = "MULTI_SHOT";
    @Setting
    private int amount;
    @Setting
    private long maxPowerTime = 1000L;
    @Setting
    private double maxPower = 3;
    @Setting
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
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (hasAbility(event.getBow())) {
                event.setCancelled(true);
                Entity entity = event.getProjectile();
                if (entity instanceof Projectile projectile){
                    projectile.remove();
                    activateAbility(new MultiShotEvent(player, event.getForce()));
                }
            }
        }
    }

    public void shoot(Player player, double force) {
        double multiplier = force*1000 / maxPowerTime * maxPower;
        int yaw = 0;
        for (int i = 0; i < amount; i++) {
            Vector vector = BukkitUtils.getVector(player, yaw, 0, multiplier);
            DamageCalculation calculation = new PlayerDamageCalculation(player);
            Arrow arrow = DamageManager.getInstance().launchProjectile(player, Arrow.class, calculation, vector);
            arrow.addScoreboardTag(BOW_TAG);
            if (i % 2 == 0) {
                yaw = Math.abs(yaw) + yawDiff;
            } else {
                yaw = -yaw;
            }
        }
    }

    @Override
    protected boolean useAbility(PlayerEvent playerEvent) {
        double force = playerEvent instanceof MultiShotEvent event ? event.force : 1;
        shoot(playerEvent.getPlayer(), force);
        return true;
    }

    @Override
    public void abilityFailedCooldown(Player player, long cooldown) {

    }

    private static final class MultiShotEvent extends PlayerEvent {
        private static final HandlerList HANDLERS = new HandlerList();
        private final float force;

        private MultiShotEvent(Player player, float force) {
            super(player);
            this.force = force;
        }

        public static HandlerList getHandlerList() {
            return HANDLERS;
        }

        @Override
        public HandlerList getHandlers() {
            return HANDLERS;
        }
    }
}
