package me.lidan.cavecrawlers.stats;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

@Getter
public class StatsCalculateEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();
    private final Stats stats;

    public StatsCalculateEvent(Player player, Stats stats) {
        super(player);
        this.player = player;
        this.stats = stats;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
