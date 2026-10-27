package me.lidan.cavecrawlers.utils;

import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Non-cancellable observation of a completed domain operation. */
@Getter
public final class CurrencyTransactionEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final UUID owner;
    private final double amount;
    private final boolean created;
    private final String reason;
    private final String source;

    public CurrencyTransactionEvent(UUID owner, double amount, boolean created, String reason, String source) {
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
