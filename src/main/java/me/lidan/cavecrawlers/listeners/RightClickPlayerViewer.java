package me.lidan.cavecrawlers.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;


public class RightClickPlayerViewer implements Listener {

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType().isAir()) {
            Player clickedPlayer = Bukkit.getPlayer(event.getRightClicked().getName());
            if (clickedPlayer != null && clickedPlayer.isOnline()) {
                player.performCommand("ct playerviewer " + clickedPlayer.getName());
            }
        }
    }
}