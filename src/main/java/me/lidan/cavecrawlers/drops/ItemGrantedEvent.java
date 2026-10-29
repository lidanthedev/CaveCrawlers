package me.lidan.cavecrawlers.drops;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/** Non-cancellable observation of a completed domain operation. */
@Getter
public final class ItemGrantedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final UUID owner;
    private final String itemId;
    private final int amount;
    private final String reason;

    public ItemGrantedEvent(UUID owner, String itemId, int amount, String reason) {
        super(!Bukkit.isPrimaryThread());
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.itemId = Objects.requireNonNull(itemId, "itemId");
        this.amount = amount;
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public static HandlerList getHandlerList() { return handlers; }
    @Override public @NotNull HandlerList getHandlers() { return handlers; }
}
