package me.lidan.cavecrawlers.utils;

import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Cancellable request emitted before the corresponding gameplay operation. */
@Getter
public final class CurrencyTransactionPrepareEvent extends Event implements org.bukkit.event.Cancellable {
    private boolean cancelled;
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    private static final HandlerList handlers = new HandlerList();
    private final UUID owner;
    private final double amount;
    private final boolean created;
    private final String reason;
    private final String source;

    public CurrencyTransactionPrepareEvent(UUID owner, double amount, boolean created, String reason, String source) {
        super(!Bukkit.isPrimaryThread());
        if (!Double.isFinite(amount) || amount <= 0) throw new IllegalArgumentException("amount must be positive and finite");
        this.owner = java.util.Objects.requireNonNull(owner, "owner");
        this.amount = amount;
        this.created = created;
        this.reason = java.util.Objects.requireNonNull(reason, "reason");
        this.source = java.util.Objects.requireNonNull(source, "source");
    }

    public static HandlerList getHandlerList() { return handlers; }
    @Override public @NotNull HandlerList getHandlers() { return handlers; }
}
