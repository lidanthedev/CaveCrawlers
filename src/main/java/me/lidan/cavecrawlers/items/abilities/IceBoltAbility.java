package me.lidan.cavecrawlers.items.abilities;

import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.damage.AbilityDamage;
import me.lidan.cavecrawlers.stats.StatType;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class IceBoltAbility extends ScalingClickAbility implements Listener {
    private static final Set<ArmorStand> activeBolts = new HashSet<>();
    private static final double SPEED = 0.8;
    private static final int MAX_DISTANCE = 20;
    public IceBoltAbility() {
        super(
                "Ice Bolt",
                "§fShoots 1 Ice Bolt that deals 1,000 damage and slows enemies hit for 5 seconds.",
                50,
                100,
                StatType.INTELLIGENCE
        );
    }

    @Override
    protected boolean useAbility(PlayerEvent playerEvent) {
        shootIceBolt(playerEvent.getPlayer());
        return true;
    }

    private void shootIceBolt(Player player) {
        Location loc = player.getEyeLocation();
        Vector direction = loc.getDirection().normalize().multiply(SPEED);
        ArmorStand bolt = loc.getWorld().spawn(loc, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setMarker(false);
            stand.setSmall(true);
            stand.setInvulnerable(true);
            stand.setHelmet(new ItemStack(Material.BLUE_ICE));
        });
        activeBolts.add(bolt);
        moveBolt(player, bolt, direction);
    }

    private void moveBolt(Player shooter, ArmorStand bolt, Vector direction) {
        new BukkitRunnable() {
            int ticksLived = 0;
            @Override
            public void run() {
                if (bolt.isDead() || ticksLived >= MAX_DISTANCE) {
                    bolt.remove();
                    activeBolts.remove(bolt);
                    cancel();
                    return;
                }
                Location newLoc = bolt.getLocation().add(direction);
                if (newLoc.getBlock().getType().isSolid()) {
                    BreakBolt(bolt);
                    cancel();
                    return;
                }
                bolt.teleport(newLoc);
                newLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, newLoc, 5, 0.1, 0.1, 0.1, 0);
                if (checkCollision(shooter, bolt)) {
                    BreakBolt(bolt);
                    cancel();
                    return;
                }
                ticksLived++;
            }
        }.runTaskTimer(CaveCrawlers.getInstance(), 1, 1);
    }

    private void BreakBolt(ArmorStand bolt) {
        Location loc = bolt.getLocation();
     //   loc.getWorld().playSound(loc, Sound.ENTITY_SNOWBALL_THROW, 1.0f, 1.0f); Disabled the sound just for you lidan.
        loc.getWorld().spawnParticle(Particle.SNOWBALL, loc, 10, 0.2, 0.2, 0.2, 0.1);
        bolt.remove();
    }


    private boolean checkCollision(Player player, ArmorStand bolt) {
        List<Entity> nearbyEntities = bolt.getNearbyEntities(1, 1, 1);
        AbilityDamage calculation = getDamageCalculation(player);
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Mob mob) {
                calculation.damage(player, mob);
                bolt.getWorld().playSound(bolt.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1.0f, 1.0f);
                bolt.getWorld().spawnParticle(Particle.SNOWBALL, bolt.getLocation(), 10, 0.2, 0.2, 0.2, 0.1);
                bolt.remove();
                break;
            }
        }
        return false;
    }
}
