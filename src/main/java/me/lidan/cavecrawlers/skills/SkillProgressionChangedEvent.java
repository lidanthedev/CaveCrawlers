package me.lidan.cavecrawlers.skills;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/** Non-cancellable observation of a completed domain operation. */
@Getter
public final class SkillProgressionChangedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final UUID owner;
    private final String skillId;
    private final int previousLevel;
    private final int level;
    private final double xpGained;

    public SkillProgressionChangedEvent(UUID owner, String skillId, int previousLevel, int level, double xpGained) {
        super(!Bukkit.isPrimaryThread());
        this.owner = Objects.requireNonNull(owner, "owner");
        this.skillId = Objects.requireNonNull(skillId, "skillId");
        this.previousLevel = previousLevel;
        this.level = level;
        this.xpGained = xpGained;
    }

    public static HandlerList getHandlerList() { return handlers; }
    @Override public @NotNull HandlerList getHandlers() { return handlers; }
}
