package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

public class ArrowSpiralAbility extends ClickAbility implements Listener {

    public ArrowSpiralAbility() {
        super("Arrow Spiral", "Shoots an arrow with a Spiral around it", 0, 0);
    }

    private Particle mainParticle = Particle.SOUL;
    private Particle[] sideParticles = {Particle.SOUL_FIRE_FLAME, Particle.FLAME};

    private double maxDistance = 50;
    private double radius = 2;
    private double rps = 6;
    private double velocity = 1.5;
    private double amount = 8;
    private double period = 0.02;

    private final long DELAY = 1L;
    private final Vector GRAVITY = new Vector(0, -20, 0); // gravity for arrow 20 m/s^2

    @Override
    protected boolean useAbility(PlayerEvent playerEvent) {
        Player player = playerEvent.getPlayer();
        Location location = player.getEyeLocation();
        World world = player.getWorld();

        Projectile p = player.launchProjectile(Arrow.class);
        p.setVelocity(location.getDirection().multiply(velocity));


        new BukkitRunnable() {
            int iteration = 0;
            double lastTime = 0;
            @Override
            public void run() {
                double time = (iteration * DELAY) / 20.0;
                if (time * velocity >= maxDistance || p.isDead()) {
                    cancel();
                    return;
                }

                Location currentLocation = p.getLocation();
                Vector vector = p.getVelocity().normalize();

                Vector arbitrary = new Vector(1, 0, 0);
                if (vector.clone().crossProduct(arbitrary).length() == 0) {
                    arbitrary = new Vector(0, 1, 0);
                }

                Vector perp1 = vector.clone().crossProduct(arbitrary).normalize();
                Vector perp2 = vector.clone().crossProduct(perp1).normalize();

                for (double i = 0; i < DELAY / 20.0; i += period) {
                    double currentTime = time + i;
                    Location futureLocation = currentLocation.clone()
                            .add(p.getVelocity().multiply(20 * i))
                            .add(GRAVITY.clone().multiply(0.5 * i * i));

                    for (int j = 0; j < amount; j++) {
                        double angle = (j / amount) * (2 * Math.PI);
                        angle += rps * currentTime;

                        Vector circleVector = perp1.clone().multiply(Math.cos(angle))
                                .add(perp2.clone().multiply(Math.sin(angle)))
                                .multiply(radius);

                        world.spawnParticle(sideParticles[j % 2], futureLocation.clone().add(circleVector), 1, 0, 0, 0, 0);
                    }
                    world.spawnParticle(mainParticle, currentLocation, 1, 0, 0, 0, 0);
                }

                lastTime = time;
                iteration++;
            }
        }.runTaskTimer(CaveCrawlers.getInstance(), 0L, DELAY);

        return true;
    }

    @Override
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        ArrowSpiralAbility ability = (ArrowSpiralAbility) super.buildAbilityWithSettings(map);
        if (map.has("maxDistance")) {
            ability.maxDistance = map.get("maxDistance").getAsDouble();
        }
        if (map.has("radius")) {
            ability.radius = map.get("radius").getAsDouble();
        }
        if (map.has("rps")) {
            ability.rps = map.get("rps").getAsDouble();
        }
        if (map.has("velocity")) {
            ability.velocity = map.get("velocity").getAsDouble();
        }
        if (map.has("amount")) {
            ability.amount = map.get("amount").getAsDouble();
        }
        if (map.has("maxDistance")) {
            ability.maxDistance = map.get("maxDistance").getAsDouble();
        }
        if (map.has("period")) {
            ability.period = map.get("period").getAsDouble();
        }
        if (map.has("mainParticle")) {
            ability.mainParticle = Particle.valueOf(map.get("mainParticle").getAsString());
        }
        if (map.has("sideParticles")) {
            ability.sideParticles = new Particle[map.get("sideParticles").getAsJsonArray().size()];
            int i = 0;
            for (JsonElement str : map.get("sideParticles").getAsJsonArray()) {
                ability.sideParticles[i] = Particle.valueOf(str.getAsString());
                i++;
            }
        }
        return ability;
    }
}
