package me.lidan.cavecrawlers.items.abilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.Vector;

public class GoldenLaserAbility extends ClickAbility implements Listener  {
    public GoldenLaserAbility() {
        super("Golden Laser", "Shoots golden laster", 100, 10);
    }

    @Override
    protected boolean useAbility(PlayerEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        Vector vector = loc.clone().getDirection();
        World world = loc.getWorld();
        loc = loc.add(vector);
        for(int i = 0 ; i < 100 ; i++) {
            loc = loc.multiply(0.1);
            world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0);
        }
        return true;
    }
}
