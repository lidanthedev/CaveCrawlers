package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class FireSpiralAbility extends ClickAbility {

    private static final double PERIOD = 0.05;
    private static final double RADIANS_PER_SECOND = Math.PI * 3;
    private static final double METERS_PER_SECOND = 1.5;
    private static final double RADIUS = 1;
    private static final double DISTANCE = 8;

    public FireSpiralAbility() {
        super("Fire Spiral", "Shoots a spiral of fire!", 175, 2000);
    }

    @Override
    protected void useAbility(PlayerEvent event) {
        Player player = event.getPlayer();

        Location newLocation = player.getEyeLocation();

        new BukkitRunnable() {
            double angle = 0;
            double blocksAhead = 1;

            @Override
            public void run() {
                if (blocksAhead >= DISTANCE) {
                    cancel();
                }

                Location location = newLocation.clone();
                location.add(location.getDirection().multiply(blocksAhead));
                location.add(new Vector(Math.cos(angle), Math.sin(angle), 0).multiply(RADIUS).rotateAroundY(Math.toRadians(-location.getYaw())));
                player.getWorld().spawnParticle(Particle.FLAME, location, 1, 0, 0, 0, 0.01);

                blocksAhead += METERS_PER_SECOND * PERIOD;
                angle += RADIANS_PER_SECOND * PERIOD;
            }
        }.runTaskTimer(CaveCrawlers.getInstance(), 0, (long) (PERIOD * 20));
    }
}
