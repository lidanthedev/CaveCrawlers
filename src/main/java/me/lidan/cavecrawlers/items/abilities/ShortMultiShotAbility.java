package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.damage.DamageCalculation;
import me.lidan.cavecrawlers.damage.DamageManager;
import me.lidan.cavecrawlers.damage.PlayerDamageCalculation;
import me.lidan.cavecrawlers.utils.BukkitUtils;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.Vector;


public class ShortMultiShotAbility extends ShortBowAbility {
    public static final String BOW_TAG = "MULTI_SHOT";
    private final int amount;
    private long maxPowerTime = 1000L;
    private double maxPower = 3;
    private int yawDiff = 5;

    public ShortMultiShotAbility(int amount) {
        this.amount = amount;
    }

    public ShortMultiShotAbility(int amount, long maxPowerTime) {
        this.amount = amount;
        this.maxPowerTime = maxPowerTime;
    }

    public ShortMultiShotAbility(int amount, long maxPowerTime, double maxPower, int yawDiff) {
        this.amount = amount;
        this.maxPowerTime = maxPowerTime;
        this.maxPower = maxPower;
        this.yawDiff = yawDiff;
    }

    @Override
    protected void useAbility(PlayerEvent playerEvent) {
        shoot(playerEvent.getPlayer(), 1);
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
}
