package me.lidan.cavecrawlers.storage.db;

import org.jdbi.v3.core.Handle;

import java.util.UUID;

/**
 * A database table that also participates in the per-player data lifecycle.
 *
 * <p>Callbacks run asynchronously and must not use thread-confined Bukkit APIs.
 * The supplied handle belongs to the same transaction as skills and ownership
 * validation. Throwing rolls back the complete logical player operation.
 */
public abstract class PlayerDataSqlTable extends SqlTable {
    public abstract void loadForPlayer(Handle handle, UUID playerUuid);

    public abstract void saveForPlayer(Handle handle, UUID playerUuid);

    public void resetForPlayer(Handle handle, UUID playerUuid) {
    }
}
