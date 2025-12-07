package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class AntiExplodeListener implements Listener {

    public static final CaveCrawlers plugin = CaveCrawlers.getInstance();
    public static final boolean ALLOW_EXPLOSIONS = plugin.getConfig().getBoolean("vanilla.explosions", false);

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        if (ALLOW_EXPLOSIONS) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (ALLOW_EXPLOSIONS) return;
        event.setCancelled(true);
    }
}
