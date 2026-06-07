package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.utils.Cooldown;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class UpdateItemsListener implements Listener {

    private final ItemsManager itemsManager = ItemsManager.getInstance();
    private final StatsManager statsManager = StatsManager.getInstance();
    private final Cooldown<UUID> updateCooldown = new Cooldown<>(1000L);

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        itemsManager.updatePlayerInventory(event.getPlayer());
        statsManager.loadPlayer(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (!updateCooldown.isCooldownFinished(event.getPlayer().getUniqueId())) return;
        updateCooldown.startCooldown(event.getPlayer().getUniqueId());
        itemsManager.updatePlayerInventory(event.getPlayer());
        statsManager.loadPlayer(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!updateCooldown.isCooldownFinished(event.getPlayer().getUniqueId())) return;
                updateCooldown.startCooldown(event.getPlayer().getUniqueId());
                itemsManager.updatePlayerInventory(event.getPlayer());
                statsManager.loadPlayer(event.getPlayer());
            }
        }.runTaskLater(CaveCrawlers.getInstance(), 1);
    }
}
