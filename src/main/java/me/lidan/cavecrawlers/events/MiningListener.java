package me.lidan.cavecrawlers.events;

import me.lidan.cavecrawlers.mining.MiningManager;
import me.lidan.cavecrawlers.stats.Stat;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.utils.BukkitUtils;
import me.lidan.cavecrawlers.utils.Cooldown;
import me.lidan.cavecrawlers.utils.RandomUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;

import java.util.List;
import java.util.UUID;

public class MiningListener implements Listener {

    private final MiningManager miningManager = MiningManager.getInstance();
    private final Cooldown<UUID> hammerCooldown = new Cooldown<>();

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SURVIVAL) {
            miningManager.breakBlock(player, event.getBlock());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SURVIVAL) {
            // what to do when a block is broken?

            handleHammer(player, event.getBlock());
            // TODO: add block drop system
        }
    }

    private void handleHammer(Player player, Block origin) {
        if (hammerCooldown.getCurrentCooldown(player.getUniqueId()) < 100){
            return;
        }
        hammerCooldown.startCooldown(player.getUniqueId());
        Stats stats = StatsManager.getInstance().getStats(player);
        Stat hammer = stats.get(StatType.MINING_HAMMER);
        double hammerLeft = hammer.getValue();
        int hammerSize = (int) Math.min((hammerLeft/50)+1, 6);
        List<Block> blocks = BukkitUtils.loopBlocks(origin.getLocation(), hammerSize);
        Material originType = origin.getType();
        for (Block block : blocks) {
            if (block.getType() == originType){
                if (hammerLeft <= 1){
                    return;
                }
                if (RandomUtils.chanceOf(hammerLeft)){
                    player.sendMessage("HAMMER!!!");
                    player.breakBlock(block);
                }
                hammerLeft /= 2;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamageAbort(BlockDamageAbortEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SURVIVAL) {
            miningManager.setProgress(player, null);
        }
    }
}
