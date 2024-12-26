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

    private double projectileVelocity = 2;
    private double maxDistance = 100;
    private double resolution = 10;
    private double amountOfLines = 8;
    private double loopiness = 1;
    private double radius = 1;

    private Particle mainParticle = Particle.SOUL;
    private Particle[] sideParticles = {Particle.SOUL_FIRE_FLAME, Particle.FLAME};

    @Override
    protected boolean useAbility(PlayerEvent playerEvent) {

        Player player = playerEvent.getPlayer();
        Location loc = player.getEyeLocation();
        World world = player.getWorld();

        Projectile p = player.launchProjectile(Arrow.class);
        p.setVelocity(loc.getDirection().multiply(projectileVelocity));

        new BukkitRunnable() {
            private final double relativeRadius = radius / 2;
            private final double mainParticleSpread = radius / 50;
            private final double lineSpacing = 360 / amountOfLines;
            private final double resolutionRatio = 1 / resolution;

            int i = 0;

            @Override
            public void run() {
                Location newloc = p.getLocation();
                for (int f = 0; f < resolution; f++) {
                    Vector vector = p.getVelocity();
                    newloc.add(vector.multiply(resolutionRatio));
                    if (p.getLocation().distance(player.getLocation()) > 2) {
                        world.spawnParticle(mainParticle, newloc, 2, 0, 0, 0, mainParticleSpread);
                    }

                    for (int a = 0, particleIndex = 0; a < amountOfLines; a++, particleIndex++) {
                        if (particleIndex >= sideParticles.length) particleIndex = 0;
                        Location loc2 = newloc.clone();

                        float angle = (float) Math.toRadians(i * loopiness + a * lineSpacing);
                        loc2.add(new Vector(Math.cos(angle), Math.sin(angle), Math.cos(angle)).multiply(relativeRadius).rotateAroundY(Math.toRadians(loc2.getPitch())));
                        loc2.add(new Vector(Math.cos(angle), Math.sin(angle), Math.cos(angle)).multiply(relativeRadius).rotateAroundY(Math.toRadians(-loc2.getPitch())));

                        world.spawnParticle(sideParticles[particleIndex], loc2, 1, 0, 0, 0, 0);
                    }
                    i++;
                }
                if (p.getLocation().distance(player.getLocation()) > maxDistance || p.isDead()) {
                    p.remove();
                    cancel();
                }
            }
        }.runTaskTimer(CaveCrawlers.getInstance(), 0, 1);
        return true;
    }

    @Override
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        ArrowSpiralAbility ability = (ArrowSpiralAbility) super.buildAbilityWithSettings(map);
        if (map.has("amountOfLines")) {
            ability.amountOfLines = map.get("amountOfLines").getAsDouble();
        }
        if (map.has("maxDistance")) {
            ability.maxDistance = map.get("maxDistance").getAsDouble();
        }
        if (map.has("radius")) {
            ability.radius = map.get("radius").getAsDouble();
        }
        if (map.has("loopiness")) {
            ability.loopiness = map.get("loopiness").getAsDouble();
        }
        if (map.has("projectileVelocity")) {
            ability.projectileVelocity = map.get("projectileVelocity").getAsDouble();
        }
        if (map.has("resolution")) {
            ability.resolution = map.get("resolution").getAsDouble();
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
