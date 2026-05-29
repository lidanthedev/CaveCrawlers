package me.lidan.cavecrawlers.storage;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired before a player's CaveCrawlers data is written to the database.
 *
 * <p>May fire on an async thread — handlers must be async-safe.
 */
@Getter
public class PlayerDataSaveEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final UUID playerUuid;

    public PlayerDataSaveEvent(UUID playerUuid) {
        super(!Bukkit.isPrimaryThread());
        this.playerUuid = playerUuid;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
