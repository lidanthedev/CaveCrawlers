package me.lidan.cavecrawlers.items.abilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.Vector;

public class ArrowAbility extends ClickAbility implements Listener {

    public ArrowAbility() {
        super("Arrow Thing", "ERROR ARROW ERROR ARROW", 0, 0);
    }

    @Override
    protected boolean useAbility(PlayerEvent playerEvent) {
        Player player = playerEvent.getPlayer();
        Location loc =  player.getEyeLocation();
        World world = player.getWorld();
        Location newloc = loc.clone();
        for(int i = 0 ;i < 1000 ; i++) {
            Vector vector = newloc.getDirection();
            newloc.add(vector.multiply(0.01).normalize());
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, newloc, 1, 0, 0, 0, 0);
            vector.setY(vector.getY()+90);
        }
        return true;
    }
}
