package me.lidan.cavecrawlers.items.abilities;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class ErrorScytheAbility extends ItemAbility implements Listener {
    public ErrorScytheAbility() {
        super("Error", "Shoot error", 5, 200);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hasAbility(hand)){
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK){
                activateAbility(player);
            }
        }
    }

    @Override
    protected void useAbility(Player player) {
        if (player.isSneaking()){
            player.launchProjectile(WitherSkull.class);
        }
        else{
            player.launchProjectile(Arrow.class);
        }
    }


}
