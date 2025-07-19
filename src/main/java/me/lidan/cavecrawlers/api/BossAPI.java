package me.lidan.cavecrawlers.api;

import me.lidan.cavecrawlers.bosses.BossDrops;

/**
 * API for managing boss-related features in the CaveCrawlers plugin.
 * Implementations should provide methods for registering, spawning, and handling bosses.
 */
public interface BossAPI {
    /**
     * Registers boss drops for a specific entity name.
     *
     * @param entityName  the name of the boss entity
     * @param entityDrops the BossDrops to register for the entity
     */
    void registerEntityDrops(String entityName, BossDrops entityDrops);

    /**
     * Retrieves the BossDrops for a given entity name.
     *
     * @param name the name of the boss entity
     * @return the BossDrops associated with the entity, or null if not found
     */
    BossDrops getEntityDrops(String name);
}
