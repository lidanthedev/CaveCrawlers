package me.lidan.cavecrawlers.api;

import me.lidan.cavecrawlers.mining.BlockInfo;
import me.lidan.cavecrawlers.mining.MiningProgress;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * API for mining-related features in the CaveCrawlers plugin.
 * Implementations should provide methods for handling custom mining logic and events.
 */
public interface MiningAPI {
    void registerBlock(Material block, BlockInfo blockInfo);

    MiningProgress getProgress(Player player);

    void setProgress(Player player, @Nullable MiningProgress progress);

    void breakBlock(Player player, Block block);
}
