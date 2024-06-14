package me.lidan.cavecrawlers.listeners;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class AntiStupidStuffListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK){
            Material clickedMat = event.getClickedBlock().getType();
            if (clickedMat.toString().contains("ANVIL") || clickedMat == Material.ENCHANTING_TABLE){
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        event.setCancelled(true);
        player.sendMessage("§cYou can't drop items!");
        player.sendMessage("§cUse /trade to trade items with other players!");
        player.sendMessage("§cUse /trash get rid off items!");
    }
}
