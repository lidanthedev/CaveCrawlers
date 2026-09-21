package me.lidan.cavecrawlers.items;

import lombok.Getter;
import lombok.Setter;
import me.lidan.cavecrawlers.items.abilities.ItemAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class PlayerItemAbilityUseEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final ItemAbility itemAbility;
    private boolean isCooldown;
    private double cost;
    private boolean cancelled;

    public PlayerItemAbilityUseEvent(@NotNull Player player, ItemAbility itemAbility, boolean isCooldown, double cost, boolean cancelled) {
        super(player);
        this.itemAbility = itemAbility;
        this.isCooldown = isCooldown;
        this.cost = cost;
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
