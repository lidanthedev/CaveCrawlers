package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpiritSpectreAbility extends ClickAbility {

    private static final long PERIOD = 1;

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

        new BukkitRunnable() {
            Location lastLocation = null;

            @Override
            public void run() {
                if (bat.isDead()) cancel();

                Location newLocation = bat.getLocation().add(0, 0.45, 0);
                newLocation.add(newLocation.getDirection().multiply(0.5));

                if (lastLocation != null && lastLocation.distance(bat.getLocation()) < 1.7) {
                    player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, bat.getLocation(), 10, 0, 0, 0, 3);

                    // TODO: add damage

                    bat.remove();
                    cancel();
                }
                bat.setVelocity(player.getLocation().getDirection().multiply(2));

                lastLocation = bat.getLocation();
            }
        }.runTaskTimer(CaveCrawlers.getInstance(), 0, PERIOD);
    }

//    private boolean isAgainstBlock(Location location) {
//        return location.clone().add(0, 0.5, 0).getBlock().getType().isSolid() ||
//                location.clone().add(0, -0.5, 0).getBlock().getType().isSolid() ||
//                location.clone().add(location.getDirection().clone().setX(0)).getBlock().getType().isSolid() ||
//                location.clone().add(location.getDirection().clone().setZ(0)).getBlock().getType().isSolid();
//    }
}
