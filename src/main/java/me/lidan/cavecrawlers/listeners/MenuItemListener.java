package me.lidan.cavecrawlers.listeners;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class MenuItemListener implements Listener {

    public static final String SERVER_GUIDE_NAME = "§6Server Guide §7(Right Click)";
    ItemStack item;

    public MenuItemListener() {
        item = ItemBuilder.from(Material.NETHER_STAR).setName(SERVER_GUIDE_NAME).build();
        Bukkit.getScheduler().scheduleSyncRepeatingTask(CaveCrawlers.getInstance(), this::playersTick, 0, 20);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getItem() == null) {
            return;
        }
        if (event.getItem().getItemMeta() == null) {
            return;
        }
        String displayName = event.getItem().getItemMeta().getDisplayName();
        if (displayName.equals(SERVER_GUIDE_NAME)) {
            player.performCommand("menu");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().getItemMeta().getDisplayName().equals(SERVER_GUIDE_NAME)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) {
            return;
        }
        if (event.getCurrentItem().getItemMeta() == null) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
//        player.sendMessage("Slot: " + event.getSlot() + " Click: " + event.getClick() + " Type: " + event.getSlotType() + " Action: " + event.getAction() + " Cursor: " + event.getCursor() + " Current: " + event.getCurrentItem() + " Hotbar: " + event.getHotbarButton() + " RawSlot: " + event.getRawSlot() + " View: " + event.getView());
        if (event.getCurrentItem().getItemMeta().getDisplayName().equals(SERVER_GUIDE_NAME)) {
            event.setCancelled(true);
            if (event.getSlot() != 8){
                event.setCurrentItem(null);
            }
        }
    }

    public void putMenuInHotbar(Player player) {
        player.getInventory().setItem(8, item);
    }

    public void playersTick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            putMenuInHotbar(player);
        }
    }
}
