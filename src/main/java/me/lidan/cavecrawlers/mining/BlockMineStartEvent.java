package me.lidan.cavecrawlers.mining;

import lombok.Getter;
import me.lidan.cavecrawlers.items.ItemInfo;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

@Getter
public class BlockMineStartEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final Block block;
    private final BlockInfo blockInfo;
    private final ItemInfo tool;
    private long requiredTicks;
    private boolean cancelled;

    public BlockMineStartEvent(@NotNull Player player, @NotNull Block block, @NotNull BlockInfo blockInfo,
                               @NotNull ItemInfo tool, long requiredTicks) {
        super(player);
        this.block = block;
        this.blockInfo = blockInfo;
        this.tool = tool;
        setRequiredTicks(requiredTicks);
    }

    public void setRequiredTicks(long requiredTicks) {
        if (requiredTicks < 0) {
            throw new IllegalArgumentException("required ticks cannot be negative");
        }
        this.requiredTicks = requiredTicks;
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
