package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class FireSpiralAbility extends ScalingClickAbility {

    @Setting
    private double period = 0.05;
    @Setting
    private double radiansPerSecond = Math.PI * 3;
    @Setting
    private double metersPerSecond = 1.5;
    @Setting
    private double radius = 1;
    @Setting
    private double distance = 8;

    public FireSpiralAbility() {
        super("Fire Spiral", "Shoots a spiral of fire!", 175, 2000);
    }

    @Override
    protected boolean useAbility(PlayerEvent event) {
        Player player = event.getPlayer();

        Location newLocation = player.getEyeLocation();

        new BukkitRunnable() {
            double angle = 0;
            double blocksAhead = 1;

            @Override
            public void run() {
                if (blocksAhead >= distance) {
                    cancel();
                }

                Location location = newLocation.clone();
                location.add(location.getDirection().multiply(blocksAhead));
                location.add(new Vector(Math.cos(angle), Math.sin(angle), 0).multiply(radius).rotateAroundY(Math.toRadians(-location.getYaw())));
                player.getWorld().spawnParticle(Particle.FLAME, location, 1, 0, 0, 0, 0.01);

                blocksAhead += metersPerSecond * period;
                angle += radiansPerSecond * period;
            }
        }.runTaskTimer(CaveCrawlers.getInstance(), 0, (long) (period * 20));
        return true;
    }

}
