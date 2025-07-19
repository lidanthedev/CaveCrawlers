package me.lidan.cavecrawlers.api;

import me.lidan.cavecrawlers.drops.EntityDrops;

/**
 * API for managing item drops in the CaveCrawlers plugin.
 * Implementations should provide methods for customizing and handling drops from entities and blocks.
 */
public interface DropsAPI {
    /**
     * Registers drops for a specific entity name.
     *
     * @param entityName  the name of the entity
     * @param entityDrops the EntityDrops to register for the entity
     */
    void register(String entityName, EntityDrops entityDrops);

    /**
     * Retrieves the EntityDrops for a given entity name.
     *
     * @param entityName the name of the entity
     * @return the EntityDrops associated with the entity, or null if not found
     */
    EntityDrops getEntityDrops(String entityName);
}
