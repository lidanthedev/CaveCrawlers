package me.lidan.cavecrawlers.storage;

import lombok.Getter;
import me.lidan.cavecrawlers.skills.Skills;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired before a player's skills are written to the database.
 * Addons listen to this event to save their own per-player data at the same time.
 *
 * <p>May fire on an async thread — handlers must be async-safe.
 */
@Getter
public class PlayerSkillsSaveEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final UUID playerUuid;
    private final Skills skills;

    public PlayerSkillsSaveEvent(UUID playerUuid, Skills skills) {
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
