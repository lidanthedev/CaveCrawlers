package me.lidan.cavecrawlers.api;

import me.lidan.cavecrawlers.mining.BlockInfo;
import me.lidan.cavecrawlers.mining.MiningRunnable;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * API for mining-related features in the CaveCrawlers plugin.
 * Implementations should provide methods for handling custom mining logic and events.
 */
public interface MiningAPI {
    /**
     * Registers a block with custom mining info.
     *
     * @param block     the Material of the block to register
     * @param blockInfo the BlockInfo containing mining data
     */
    void registerBlock(Material block, BlockInfo blockInfo);

    /**
     * Gets the mining runnable for a player.
     *
     * @param player the player whose mining runnable is requested
     * @return the MiningRunnable for the player
     */
    MiningRunnable getProgress(Player player);

    /**
     * Sets the mining-runnable object for a player.
     *
     * @param player the player whose mining runnable is to be set
     * @param progress the MiningRunnable to set (nullable)
     */
    void setProgress(Player player, @Nullable MiningRunnable progress);

    /**
     * Breaks a block for a player using custom mining logic.
     *
     * @param player the player breaking the block
     * @param block the block to break
     */
    void breakBlock(Player player, Block block);
}
