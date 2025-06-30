package me.lidan.cavecrawlers.api;

import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * API for managing player stats in the CaveCrawlers plugin.
 * Provides methods for retrieving, applying, and calculating player stats.
 */
public interface StatsAPI {
    /**
     * Retrieves the Stats object for a player by UUID.
     *
     * @param uuid The UUID of the player.
     * @return The Stats object for the player.
     */
    Stats getStats(UUID uuid);

    /**
     * Retrieves the Stats object for a player by Player instance.
     *
     * @param player The Player instance.
     * @return The Stats object for the player.
     */
    Stats getStats(Player player);

    /**
     * Calculates the stats for the given player.
     *
     * @param player The Player to calculate stats for.
     * @return The calculated Stats object.
     */
    Stats calculateStats(Player player);

    /**
     * Registers a stat type
     *
     * @param id       the unique identifier for the stat
     * @param statType the StatType
     */
    void register(String id, StatType statType);
}
