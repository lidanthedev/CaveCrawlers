package me.lidan.cavecrawlers.damage;

import lombok.Getter;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Fired after CaveCrawlers calculates player damage and before it is applied to a mob.
 * The damage value already includes the server damage multiplier.
 */
@Getter
public class DamageCalculationEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final Mob target;
    private final @Nullable DamageCalculation calculation;
    private double damage;
    private boolean critical;
    private boolean cancelled;

    public DamageCalculationEvent(@NotNull Player player, @NotNull Mob target, @Nullable DamageCalculation calculation,
                                  double damage, boolean critical) {
        this.player = Objects.requireNonNull(player, "player");
        this.target = Objects.requireNonNull(target, "target");
        this.calculation = calculation;
        setDamage(damage);
        this.critical = critical;
    }

    public void setDamage(double damage) {
        if (!Double.isFinite(damage) || damage < 0) {
            throw new IllegalArgumentException("damage must be finite and non-negative");
        }
        this.damage = damage;
    }

    public void setCritical(boolean critical) {
        this.critical = critical;
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
