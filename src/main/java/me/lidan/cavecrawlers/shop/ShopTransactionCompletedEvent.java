package me.lidan.cavecrawlers.shop;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Non-cancellable observation of a completed domain operation. */
@Getter
public final class ShopTransactionCompletedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final UUID owner;
    private final UUID transactionId;
    private final boolean purchase;
    private final double price;
    private final Map<String, Integer> items;
    private final Map<String, Integer> ingredients;

    public ShopTransactionCompletedEvent(UUID owner, UUID transactionId, boolean purchase, double price, Map<String, Integer> items, Map<String, Integer> ingredients) {
        super(!Bukkit.isPrimaryThread());
        this.owner = Objects.requireNonNull(owner, "owner");
        this.transactionId = Objects.requireNonNull(transactionId, "transactionId");
        this.purchase = purchase;
        this.price = price;
        this.items = Map.copyOf(items);
        this.ingredients = Map.copyOf(ingredients);
    }

    public static HandlerList getHandlerList() { return handlers; }
    @Override public @NotNull HandlerList getHandlers() { return handlers; }
}
