package me.lidan.cavecrawlers.utils;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

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
        this.owner = Objects.requireNonNull(owner, "owner");
        this.amount = amount;
        this.created = created;
        this.reason = Objects.requireNonNull(reason, "reason");
        this.source = Objects.requireNonNull(source, "source");
    }

    public static HandlerList getHandlerList() { return handlers; }
    @Override public @NotNull HandlerList getHandlers() { return handlers; }
}
