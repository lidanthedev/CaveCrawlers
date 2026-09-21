package me.lidan.cavecrawlers.items;

import lombok.Getter;
import me.lidan.cavecrawlers.items.abilities.ItemAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Getter
public class PlayerItemAbilityUseEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final ItemAbility itemAbility;
    private final ItemStack itemStack;
    private final @Nullable ItemInfo itemInfo;
    private long cooldown;
    private double cost;
    private boolean cancelled;

    public PlayerItemAbilityUseEvent(@NotNull Player player, @NotNull ItemAbility itemAbility, @NotNull ItemStack itemStack,
                                     @Nullable ItemInfo itemInfo, long cooldown, double cost) {
        super(player);
        this.itemAbility = Objects.requireNonNull(itemAbility, "itemAbility");
        this.itemStack = Objects.requireNonNull(itemStack, "itemStack");
        this.itemInfo = itemInfo;
        setCooldown(cooldown);
        setCost(cost);
    }

    public void setCooldown(long cooldown) {
        if (cooldown < 0) {
            throw new IllegalArgumentException("cooldown cannot be negative");
        }
        this.cooldown = cooldown;
    }

    public void setCost(double cost) {
        if (!Double.isFinite(cost) || cost < 0) {
            throw new IllegalArgumentException("cost must be finite and non-negative");
        }
        this.cost = cost;
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
