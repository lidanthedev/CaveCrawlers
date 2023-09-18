package me.lidan.cavecrawlers.mining;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.lidan.cavecrawlers.packets.PacketManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

@Getter
public class MiningProgress extends BukkitRunnable {
    private final Player player;
    private final Block block;
    private long countTicks;
    private final long requiredTicks;

    public MiningProgress(Player player, Block block, long requiredTicks) {
        this.player = player;
        this.block = block;
        this.countTicks = 0;
        this.requiredTicks = requiredTicks;
    }

    @Override
    public void run() {
        if (countTicks > requiredTicks || requiredTicks == 0){
            player.breakBlock(block);
            cancel();
            return;
        }

        int stage = Math.round((float) countTicks / requiredTicks * 10);
        if (requiredTicks == 1 && countTicks == 0){
            stage = 9;
        }
        PacketManager.getInstance().setBlockDestroyStage(player, block.getLocation(), stage);

        countTicks++;
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        super.cancel();
        PacketManager.getInstance().setBlockDestroyStage(player, block.getLocation(), 10);
    }
}
