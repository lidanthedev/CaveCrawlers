package me.lidan.cavecrawlers.storage.db;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface PlayerSessionsDao {

    /**
     * Creates a session row if none exists; no-op otherwise.
     */
    @SqlUpdate("INSERT IGNORE INTO player_sessions " +
            "(player_uuid, is_locked, locking_server, lock_timestamp, fence_token, data_revision) " +
            "VALUES (:uuid, 0, NULL, 0, 0, 0)")
    void ensureRow(@Bind("uuid") String uuid);

    /**
     * Releases the lock, but only if {@code server} currently holds it.
     * Guards against a server accidentally releasing another server's lock.
     */
    @SqlUpdate("UPDATE player_sessions " +
            "SET is_locked = 0, locking_server = NULL, lock_timestamp = 0 " +
            "WHERE player_uuid = :uuid AND locking_server = :server AND fence_token = :fence")
    int releaseLock(@Bind("uuid") String uuid, @Bind("server") String server, @Bind("fence") long fence);

    /** Releases every lock held by this process-unique server ID. */
    @SqlUpdate("UPDATE player_sessions " +
            "SET is_locked = 0, locking_server = NULL, lock_timestamp = 0 " +
            "WHERE locking_server = :server")
    void releaseAllLocks(@Bind("server") String server);
}
