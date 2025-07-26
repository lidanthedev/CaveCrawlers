package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.mining.MiningManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class MiningListener implements Listener {

    private final MiningManager miningManager = MiningManager.getInstance();
    private final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private List<String> blacklistedWorlds = plugin.getConfig().getStringList("mining-blacklisted-worlds");

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        String name = player.getWorld().getName();
        if (blacklistedWorlds.contains(name)) {
            if (player.hasPotionEffect(PotionEffectType.MINING_FATIGUE)) {
                int duration = player.getPotionEffect(PotionEffectType.MINING_FATIGUE).getDuration();
                if (duration == -1){
                    player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
                }
            }
            return;
        }

        if (player.getGameMode() == GameMode.SURVIVAL) {
            miningManager.breakBlock(player, event.getBlock());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String name = player.getWorld().getName();
        if (blacklistedWorlds.contains(name)) {
            return;
        }
        if (player.getGameMode() == GameMode.SURVIVAL) {
            miningManager.handleBreak(event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamageAbort(BlockDamageAbortEvent event) {
        Player player = event.getPlayer();
        String name = player.getWorld().getName();
        if (blacklistedWorlds.contains(name)) {
            return;
        }
        if (player.getGameMode() == GameMode.SURVIVAL) {
            miningManager.setProgress(player, null);
        }
    }
}
