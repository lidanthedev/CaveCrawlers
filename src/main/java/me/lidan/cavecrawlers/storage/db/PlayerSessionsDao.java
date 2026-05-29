package me.lidan.cavecrawlers.storage.db;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface PlayerSessionsDao {

    /**
     * Creates a session row if none exists; no-op otherwise.
     */
    @SqlUpdate("INSERT IGNORE INTO player_sessions (player_uuid, is_locked, locking_server, lock_timestamp) " +
            "VALUES (:uuid, 0, NULL, 0)")
    void ensureRow(@Bind("uuid") String uuid);

    /**
     * Atomically acquires the lock for {@code server} if it is currently free
     * or has not been heartbeated since {@code expiry} (crash-recovery).
     *
     * @return number of rows affected — 1 means acquired, 0 means someone else holds it
     */
    /**
     * Acquires the lock if it is free, expired, or already held by {@code server}.
     * The "already held by us" clause prevents same-server concurrent loads from
     * deadlocking each other — two threads on this server share the same lock identity.
     */
    @SqlUpdate("UPDATE player_sessions " +
            "SET is_locked = 1, locking_server = :server, lock_timestamp = :ts " +
            "WHERE player_uuid = :uuid " +
            "  AND (is_locked = 0 OR lock_timestamp < :expiry OR locking_server = :server)")
    int tryAcquireLock(@Bind("uuid") String uuid, @Bind("server") String server,
                       @Bind("ts") long ts, @Bind("expiry") long expiry);

    /**
     * Releases the lock, but only if {@code server} currently holds it.
     * Guards against a server accidentally releasing another server's lock.
     */
    @SqlUpdate("UPDATE player_sessions " +
            "SET is_locked = 0, locking_server = NULL, lock_timestamp = 0 " +
            "WHERE player_uuid = :uuid AND locking_server = :server")
    void releaseLock(@Bind("uuid") String uuid, @Bind("server") String server);

    /**
     * Refreshes the lock timestamp for every row held by {@code server}.
     */
    @SqlUpdate("UPDATE player_sessions SET lock_timestamp = :ts WHERE locking_server = :server")
    void heartbeatAll(@Bind("server") String server, @Bind("ts") long ts);

    /**
     * Releases every lock held by {@code server}. Called on clean shutdown.
     */
    @SqlUpdate("UPDATE player_sessions " +
            "SET is_locked = 0, locking_server = NULL, lock_timestamp = 0 " +
            "WHERE locking_server = :server")
    void releaseAllLocks(@Bind("server") String server);
}
