package me.lidan.cavecrawlers.items.abilities;

import com.cryptomorin.xseries.XPotion;
import com.cryptomorin.xseries.particles.XParticle;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;

public class FreezeAbility extends PotionAbility{
    public FreezeAbility() {
        super("Freeze", "Freeze mobs near you", 100, 10000, 100, 5, XPotion.SLOWNESS.get(), 5, "mobs");
    }

    @Override
    protected boolean useAbility(PlayerEvent playerEvent) {
        super.useAbility(playerEvent);
        Player player = playerEvent.getPlayer();
        player.getWorld().spawnParticle(XParticle.ITEM_SNOWBALL.get(), player.getLocation(), 100, 5, 5, 5);
        return true;
    }
}
