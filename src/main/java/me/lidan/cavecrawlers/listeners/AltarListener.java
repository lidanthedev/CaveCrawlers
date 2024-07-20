package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.altar.Altar;
import me.lidan.cavecrawlers.altar.AltarDrop;
import me.lidan.cavecrawlers.items.ItemsManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;

public class AltarListener implements Listener {
    private Altar testAltar = new Altar(List.of(new Location(Bukkit.getWorld("work"), 147,67,4)), new Location(Bukkit.getWorld("work"),148,70,2), List.of(new AltarDrop(100, "TestBoss")), ItemsManager.getInstance().getItemByID("SUMMONING_EYE"), Material.END_PORTAL_FRAME, Material.BEDROCK, null);

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND){
            return;
        }
        testAltar.onPlayerInteract(event);
    }
}
