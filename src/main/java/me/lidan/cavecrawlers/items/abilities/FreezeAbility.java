package me.lidan.cavecrawlers.items.abilities;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.potion.PotionEffectType;

public class FreezeAbility extends PotionAbility{
    public FreezeAbility() {
        super("Freeze", "Freeze mobs near you", 100, 10000, 100, 5, PotionEffectType.SLOWNESS, 5, "mobs");
    }

    @Override
    protected boolean useAbility(PlayerEvent playerEvent) {
        super.useAbility(playerEvent);
        Player player = playerEvent.getPlayer();
        player.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, player.getLocation(), 100, 5, 5, 5);
        return true;
    }
}
