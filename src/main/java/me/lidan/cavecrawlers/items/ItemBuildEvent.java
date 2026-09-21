package me.lidan.cavecrawlers.items;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Getter
public class ItemBuildEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final ItemStack originalItem;
    private final ItemStack builtItem;
    private final ItemInfo itemInfo;

    public ItemBuildEvent(ItemStack originalItem, ItemStack builtItem, ItemInfo itemInfo) {
        this.originalItem = originalItem;
        this.builtItem = builtItem;
        this.itemInfo = itemInfo;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
