package me.lidan.cavecrawlers.events;

import me.lidan.cavecrawlers.items.ItemsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.LazyMetadataValue;

public class AntiPlaceListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack hand = event.getItemInHand();
        String ID = ItemsManager.getInstance().getIDofItemStack(hand);
        if (ID != null){
            event.setCancelled(true);
        }
    }
}
