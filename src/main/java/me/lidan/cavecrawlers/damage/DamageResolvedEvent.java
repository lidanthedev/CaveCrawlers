package me.lidan.cavecrawlers.damage;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Non-cancellable observation of a completed domain operation. */
@Getter
public final class DamageResolvedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final Mob target;
    private final double damage;
    private final boolean critical;
    private final boolean ability;

    public DamageResolvedEvent(Player player, Mob target, double damage, boolean critical, boolean ability) {
        super(!Bukkit.isPrimaryThread());
        this.player = Objects.requireNonNull(player, "player");
        this.target = Objects.requireNonNull(target, "target");
        this.damage = damage;
        this.critical = critical;
        this.ability = ability;
    }

    public static HandlerList getHandlerList() { return handlers; }
    @Override public @NotNull HandlerList getHandlers() { return handlers; }
}
