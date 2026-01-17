package me.lidan.cavecrawlers.listeners;

import com.cryptomorin.xseries.XAttribute;
import com.cryptomorin.xseries.XPotion;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.mining.MiningManager;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;

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
            if (player.hasPotionEffect(XPotion.MINING_FATIGUE.get())) {
                int duration = player.getPotionEffect(XPotion.MINING_FATIGUE.get()).getDuration();
                if (duration == -1){
                    player.removePotionEffect(XPotion.MINING_FATIGUE.get());
                }
                Attribute attribute = XAttribute.BLOCK_BREAK_SPEED.get();
                if (attribute != null) {
                    AttributeInstance playerAttribute = player.getAttribute(attribute);
                    if (playerAttribute.getBaseValue() == 0.0) {
                        playerAttribute.setBaseValue(1.0);
                    }
                }
            }
            return;
        }

        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }
        miningManager.breakBlock(player, event.getBlock(), event.getBlockFace());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String name = player.getWorld().getName();
        if (blacklistedWorlds.contains(name)) {
            return;
        }
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }
        miningManager.handleBreak(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamageAbort(BlockDamageAbortEvent event) {
        Player player = event.getPlayer();
        String name = player.getWorld().getName();
        if (blacklistedWorlds.contains(name)) {
            return;
        }
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }
        miningManager.setProgress(player, null);
    }
}
