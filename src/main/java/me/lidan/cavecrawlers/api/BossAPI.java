package me.lidan.cavecrawlers.api;

import me.lidan.cavecrawlers.bosses.BossDrops;

/**
 * API for managing boss-related features in the CaveCrawlers plugin.
 * Implementations should provide methods for registering, spawning, and handling bosses.
 */
public interface BossAPI {
    void registerEntityDrops(String entityName, BossDrops entityDrops);

    BossDrops getEntityDrops(String name);
}
