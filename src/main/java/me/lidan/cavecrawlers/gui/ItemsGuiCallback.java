package me.lidan.cavecrawlers.gui;

import me.lidan.cavecrawlers.items.ItemInfo;
import org.bukkit.event.inventory.InventoryClickEvent;

@FunctionalInterface
public interface ItemsGuiCallback {
    public void callback(InventoryClickEvent event, ItemInfo clickedItemInfo);
}
