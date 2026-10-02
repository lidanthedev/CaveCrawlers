package me.lidan.cavecrawlers.altar;

import lombok.Getter;
import me.lidan.cavecrawlers.items.ItemInfo;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Getter
public class AltarUseEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final Altar altar;
    private final Block block;
    private final ItemInfo item;
    private final ItemStack itemStack;
    private final boolean finalPlacement;
    private int points;
    private boolean cancelled;

    public AltarUseEvent(@NotNull Player player, @NotNull Altar altar, @NotNull Block block, @NotNull ItemInfo item,
                         @NotNull ItemStack itemStack, int points, boolean finalPlacement) {
        super(player);
        this.altar = altar;
        this.block = block;
        this.item = item;
        this.itemStack = itemStack;
        setPoints(points);
        this.finalPlacement = finalPlacement;
    }

    public void setPoints(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("points cannot be negative");
        }
        this.points = points;
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
