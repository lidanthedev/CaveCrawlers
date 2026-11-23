package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class ArrowSpiralAbility extends ClickAbility implements Listener {

    public ArrowSpiralAbility() {
        super("Arrow Spiral", "Shoots an arrow with a Spiral around it", 0, 0);
    }

    @Setting
    private double projectileVelocity = 2;
    @Setting
    private double maxDistance = 100;
    @Setting
    private double resolution = 10;
    @Setting
    private double amountOfLines = 8;
    @Setting
    private double loopiness = 1;
    @Setting
    private double radius = 1;

    @Setting
    private Particle mainParticle = Particle.SOUL;
    @Setting
    private Particle[] sideParticles = {Particle.SOUL_FIRE_FLAME, Particle.FLAME};

    @Override
    protected boolean useAbility(PlayerEvent playerEvent) {

        Player player = playerEvent.getPlayer();
        Location loc = player.getEyeLocation();
        World world = player.getWorld();

        Projectile p = player.launchProjectile(Arrow.class);
        p.setVelocity(loc.getDirection().multiply(projectileVelocity));

        new BukkitRunnable() {
            private final double mainParticleSpread = radius / 50;
            private final double lineSpacing = 360 / amountOfLines;
            private final double resolutionRatio = 1 / resolution;

            int i = ThreadLocalRandom.current().nextInt(360);

            @Override
            public void run() {
                Location newloc = p.getLocation();
                for (int f = 0; f < resolution; f++) {
                    Vector vector = p.getVelocity();
                    newloc.add(vector.multiply(resolutionRatio));
                    if (newloc.distance(player.getLocation()) > 2.5) {
                        world.spawnParticle(mainParticle, newloc, 2, 0, 0, 0, mainParticleSpread);
                        for (int a = 0, particleIndex = 0; a < amountOfLines; a++, particleIndex++) {
                            if (particleIndex >= sideParticles.length) particleIndex = 0;
                            Location loc2 = newloc.clone();

                            float angle = (float) Math.toRadians(360 - ((i * loopiness + a * lineSpacing) % 360));
                            loc2.add(new Vector(Math.cos(angle), Math.sin(angle), Math.cos(angle)).multiply(radius)
                                    .rotateAroundY(Math.toRadians(loc.getYaw()))
                                    .rotateAroundX(Math.toRadians(-p.getLocation().getPitch()))
                            );
                            world.spawnParticle(sideParticles[particleIndex], loc2, 1, 0, 0, 0, 0);
                        }
                    }
                    i++;
                }
                if (p.getLocation().distance(loc) > maxDistance || p.isDead()) {
                    p.remove();
                    cancel();
                }
            }
        }.runTaskTimer(CaveCrawlers.getInstance(), 0, 1);
        return true;
    }

}
