package me.lidan.cavecrawlers.shop;

import lombok.Getter;
import me.lidan.cavecrawlers.items.ItemInfo;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
public class ShopPurchaseEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final ShopItem shopItem;
    private double price;
    private Map<ItemInfo, Integer> ingredients;
    private int resultAmount;
    private boolean cancelled;

    public ShopPurchaseEvent(@NotNull Player player, @NotNull ShopItem shopItem, double price,
                             @NotNull Map<ItemInfo, Integer> ingredients, int resultAmount) {
        super(player);
        this.shopItem = shopItem;
        setPrice(price);
        setIngredients(ingredients);
        setResultAmount(resultAmount);
    }

    public void setPrice(double price) {
        if (!Double.isFinite(price) || price < 0) {
            throw new IllegalArgumentException("price must be finite and non-negative");
        }
        this.price = price;
    }

    public void setIngredients(@NotNull Map<ItemInfo, Integer> ingredients) {
        this.ingredients = new HashMap<>(Objects.requireNonNull(ingredients, "ingredients"));
    }

    public void setResultAmount(int resultAmount) {
        if (resultAmount < 0) {
            throw new IllegalArgumentException("result amount cannot be negative");
        }
        this.resultAmount = resultAmount;
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
