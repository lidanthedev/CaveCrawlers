package me.lidan.cavecrawlers.skills;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

@Getter
public class SkillXpGainEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Skill skill;
    @Setter
    private double xpGained;
    @Setter
    private boolean cancelled = false;

    public SkillXpGainEvent(@NotNull Player player, Skill skill, double xpGained) {
        super(player);
        this.skill = skill;
        this.xpGained = xpGained;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
