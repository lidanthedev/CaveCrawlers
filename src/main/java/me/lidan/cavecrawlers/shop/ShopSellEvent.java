package me.lidan.cavecrawlers.shop;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ShopSellEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final List<ItemStack> sellableItems;
    private final List<ItemStack> unsellableItems;
    private double price;
    private boolean trashUnsellable;
    private boolean cancelled;

    public ShopSellEvent(@NotNull Player player, @NotNull List<ItemStack> sellableItems,
                         @NotNull List<ItemStack> unsellableItems, double price, boolean trashUnsellable) {
        super(player);
        this.sellableItems = List.copyOf(new ArrayList<>(sellableItems));
        this.unsellableItems = List.copyOf(new ArrayList<>(unsellableItems));
        setPrice(price);
        this.trashUnsellable = trashUnsellable;
    }

    public void setPrice(double price) {
        if (!Double.isFinite(price) || price < 0) {
            throw new IllegalArgumentException("price must be finite and non-negative");
        }
        this.price = price;
    }

    public void setTrashUnsellable(boolean trashUnsellable) {
        this.trashUnsellable = trashUnsellable;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
