package me.lidan.cavecrawlers.listeners;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class MenuItemListener implements Listener {

    public static final String SERVER_GUIDE_NAME = "§6Server Guide §7(Right Click)";
    public static final String MENU = "menu";
    public final static boolean IS_MENU_ENABLED = CaveCrawlers.getInstance().getConfig().getBoolean("menu.item-enabled", false);

    ItemStack menuItem;

    public MenuItemListener() {
        if (!IS_MENU_ENABLED) {
            return;
        }
        menuItem = ItemBuilder.from(Material.NETHER_STAR).setName(SERVER_GUIDE_NAME).build();
        Bukkit.getScheduler().scheduleSyncRepeatingTask(CaveCrawlers.getInstance(), this::playersTick, 0, 20);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!IS_MENU_ENABLED) {
            return;
        }
        Player player = event.getPlayer();
        if (event.getItem() == null) {
            return;
        }
        if (event.getItem().getItemMeta() == null) {
            return;
        }
        String displayName = event.getItem().getItemMeta().getDisplayName();
        if (displayName.equals(SERVER_GUIDE_NAME)) {
            player.performCommand(MENU);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!IS_MENU_ENABLED) {
            return;
        }
        if (event.getItemDrop().getItemStack().getItemMeta().getDisplayName().equals(SERVER_GUIDE_NAME)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!IS_MENU_ENABLED) {
            return;
        }
        if (event.getCurrentItem() == null) {
            return;
        }
        if (event.getCurrentItem().getItemMeta() == null) {
            return;
        }
        if (event.getCurrentItem().getItemMeta().getDisplayName().equals(SERVER_GUIDE_NAME)) {
            event.setCancelled(true);
            if (event.getSlot() != 8){
                event.setCurrentItem(null);
            }
        }
    }

    public void putMenuInHotbar(Player player) {
        if (!IS_MENU_ENABLED) {
            return;
        }
        PlayerInventory playerInventory = player.getInventory();
        if (playerInventory.getItem(8) != null) {
            return;
        }
        playerInventory.setItem(8, menuItem);
    }

    public void playersTick() {
        if (!IS_MENU_ENABLED) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            putMenuInHotbar(player);
        }
    }
}
