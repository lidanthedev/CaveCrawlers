package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonObject;
import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class FireSpiralAbility extends ScalingClickAbility {

    private double period = 0.05;
    private double radiansPerSecond = Math.PI * 3;
    private double metersPerSecond = 1.5;
    private double radius = 1;
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

    @Override
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        FireSpiralAbility ability = (FireSpiralAbility) super.buildAbilityWithSettings(map);
        if (map.has("period")) {
            ability.period = map.get("period").getAsDouble();
        }
        if (map.has("radiansPerSecond")) {
            ability.radiansPerSecond = map.get("radiansPerSecond").getAsDouble();
        }
        if (map.has("metersPerSecond")) {
            ability.metersPerSecond = map.get("metersPerSecond").getAsDouble();
        }
        if (map.has("radius")) {
            ability.radius = map.get("radius").getAsDouble();
        }
        if (map.has("distance")) {
            ability.distance = map.get("distance").getAsDouble();
        }
        return ability;
    }
}
