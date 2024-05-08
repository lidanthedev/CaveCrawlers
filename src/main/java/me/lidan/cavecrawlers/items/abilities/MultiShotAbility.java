package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonObject;
import me.lidan.cavecrawlers.utils.BukkitUtils;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;


public class MultiShotAbility extends ItemAbility implements Listener {
    public static final String BOW_TAG = "MULTI_SHOT";
    private int amount;
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
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (hasAbility(event.getBow())) {
                event.setCancelled(true);
                Entity entity = event.getProjectile();
                if (entity instanceof Projectile projectile){
                    projectile.remove();
                    shoot(player, event.getForce());
                }
            }
        }
    }

    public void shoot(Player player, double force) {
        double multiplier = force*1000 / maxPowerTime * maxPower;
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

    @Override
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        MultiShotAbility ability = (MultiShotAbility) this.clone();
        if (map.has("amount")) {
            ability.amount = map.get("amount").getAsInt();
        }
        if (map.has("maxPowerTime")) {
            ability.maxPowerTime = map.get("maxPowerTime").getAsLong();
        }
        if (map.has("maxPower")) {
            ability.maxPower = map.get("maxPower").getAsDouble();
        }
        if (map.has("yawDiff")) {
            ability.yawDiff = map.get("yawDiff").getAsInt();
        }
        return ability;
    }

    @Override
    protected void useAbility(PlayerEvent playerEvent) {

    }

    @Override
    public void abilityFailedCooldown(Player player) {

    }
}
