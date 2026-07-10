package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.mining.MiningManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkUnloadListener implements Listener {

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        MiningManager.getInstance().restoreBlocksInChunk(event.getChunk());
    }
}
