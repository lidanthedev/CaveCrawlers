package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.griffin.GriffinManager;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.abilities.SpadeAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;

public class GriffinListener implements Listener {
    GriffinManager griffinManager = GriffinManager.getInstance();

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        ItemInfo itemInfo = ItemsManager.getInstance().getItemFromItemStackSafe(player.getInventory().getItemInMainHand());
        if (itemInfo == null){
            return;
        }
        if (itemInfo.getAbility() instanceof SpadeAbility){
            griffinManager.handleGriffinClick(player, event.getBlock());
        }
    }
}
