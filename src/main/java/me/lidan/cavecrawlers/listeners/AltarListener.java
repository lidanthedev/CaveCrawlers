package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.altar.Altar;
import me.lidan.cavecrawlers.altar.AltarManager;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class AltarListener implements Listener {
    private final AltarManager altarManager = AltarManager.getInstance();

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;
        ItemInfo itemInHand = ItemsManager.getInstance().getItemFromItemStackSafe(event.getItem());
        if (itemInHand == null) return;
        Altar altar = altarManager.getAltarAtLocation(clickedBlock.getLocation(), itemInHand);
        if (altar == null) return;
        event.setCancelled(true);
        altar.onPlayerInteract(event);
    }
}
