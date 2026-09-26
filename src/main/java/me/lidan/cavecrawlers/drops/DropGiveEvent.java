package me.lidan.cavecrawlers.drops;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class DropGiveEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final Drop drop;
    private final @Nullable Player player;
    private final Location location;
    private boolean cancelled;

    public DropGiveEvent(@NotNull Drop drop, @Nullable Player player, @NotNull Location location) {
        this.drop = drop;
        this.player = player;
        this.location = location;
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
