package me.lidan.cavecrawlers.items.abilities;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.potion.PotionEffectType;

public class FreezeAbility extends PotionAbility{
    public FreezeAbility() {
        super("Freeze", "Freeze mobs near you", 100, 10000, 100, 5, PotionEffectType.SLOW, 5, "mobs");
    }

    @Override
    protected void useAbility(PlayerEvent playerEvent) {
        super.useAbility(playerEvent);
        Player player = playerEvent.getPlayer();
        player.getWorld().spawnParticle(org.bukkit.Particle.SNOWBALL, player.getLocation(), 100, 5, 5, 5);
    }
}
