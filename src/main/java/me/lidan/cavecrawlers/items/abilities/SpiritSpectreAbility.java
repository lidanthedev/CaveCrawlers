package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonObject;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.damage.AbilityDamage;
import me.lidan.cavecrawlers.utils.BukkitUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpiritSpectreAbility extends ScalingClickAbility {

    private static final long PERIOD = 1;
    private int radius = 3;

    public SpiritSpectreAbility() {
        super("Spirit Spectre", "Shoots a guided spirit bat, following your aim and exploding for " + ChatColor.RED + "2,000 " + ChatColor.GRAY + "damage.", 250, 0);
    }

    @Override
    protected void useAbility(PlayerEvent event) {
        Player player = event.getPlayer();

        Bat bat = player.getWorld().spawn(player.getLocation().add(0, 0.5, 0), Bat.class);
        bat.setGravity(false);
        bat.setInvulnerable(true);
        bat.setAwake(true);
        bat.addScoreboardTag("Spirit_Sceptre");

        AbilityDamage calculation = getDamageCalculation(player);

        new BukkitRunnable() {
            Location lastLocation = null;

            @Override
            public void run() {
                if (bat.isDead()) cancel();

                Location newLocation = bat.getLocation().add(0, 0.45, 0);
                newLocation.add(newLocation.getDirection().multiply(0.5));

                if (lastLocation != null && lastLocation.distance(bat.getLocation()) < 1.7) {
                    player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, bat.getLocation(), 10, 0, 0, 0, 3);

                    BukkitUtils.getNearbyMobs(bat.getLocation(), radius).forEach(mob -> {
                        calculation.damage(player, mob);
                    });

                    bat.remove();
                    cancel();
                }
                bat.setVelocity(player.getLocation().getDirection().multiply(2));

                lastLocation = bat.getLocation();
            }
        }.runTaskTimer(CaveCrawlers.getInstance(), 0, PERIOD);
    }

    @Override
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        SpiritSpectreAbility ability = (SpiritSpectreAbility) super.buildAbilityWithSettings(map);
        if (map.has("radius")) {
            ability.radius = map.get("radius").getAsInt();
        }
        return ability;
    }
}
