package me.lidan.cavecrawlers.items.abilities;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;

public class ErrorScytheAbility extends ClickAbility implements Listener {
    public ErrorScytheAbility() {
        super("Error", "Shoot error", 5, 200);
    }

    @Override
    protected boolean useAbility(PlayerEvent playerEvent) {
        Player player = playerEvent.getPlayer();
        if (player.isSneaking()){
            player.launchProjectile(WitherSkull.class);
        }
        else{
            player.launchProjectile(Arrow.class);
        }
        return true;
    }


}
