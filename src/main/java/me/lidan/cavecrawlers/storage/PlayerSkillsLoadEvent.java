package me.lidan.cavecrawlers.storage;

import lombok.Getter;
import me.lidan.cavecrawlers.skills.Skills;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after a player's skills are loaded into the cache and are ready to use.
 * Addons listen to this event to load their own per-player data on join.
 *
 * <p>May fire on an async thread — handlers must be async-safe.
 */
@Getter
public class PlayerSkillsLoadEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final UUID playerUuid;
    private final Skills skills;

    public PlayerSkillsLoadEvent(UUID playerUuid, Skills skills) {
        super(!Bukkit.isPrimaryThread());
        this.playerUuid = playerUuid;
        this.skills = skills;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
