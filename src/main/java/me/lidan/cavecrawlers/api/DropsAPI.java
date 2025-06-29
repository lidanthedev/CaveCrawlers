package me.lidan.cavecrawlers.api;

import me.lidan.cavecrawlers.drops.EntityDrops;

/**
 * API for managing item drops in the CaveCrawlers plugin.
 * Implementations should provide methods for customizing and handling drops from entities and blocks.
 */
public interface DropsAPI {
    void register(String entityName, EntityDrops entityDrops);

    EntityDrops getEntityDrops(String entityName);
}
