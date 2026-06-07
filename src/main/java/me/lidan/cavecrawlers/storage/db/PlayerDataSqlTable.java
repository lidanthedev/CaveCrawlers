package me.lidan.cavecrawlers.storage.db;

import java.util.UUID;

/**
 * A database table that also participates in the per-player data lifecycle.
 *
 * <p>Implementations should use {@link Database#getJdbi()} to load data into
 * their own cache and save cached data back to their table. These methods may
 * run asynchronously, so implementations must avoid unsafe Bukkit API calls.
 */
public abstract class PlayerDataSqlTable extends SqlTable {
    public abstract void loadForPlayer(UUID playerUuid);

    public abstract void saveForPlayer(UUID playerUuid);
}
