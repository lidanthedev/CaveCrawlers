package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.utils.BukkitUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class EarthShooterAbility extends ClickAbility {
    private static final int RADIUS = 5;

    private final Map<UUID, List<FallingBlock>> playersBlocks = new HashMap<>();

    public EarthShooterAbility() {
        super("Earth Shooter", "Takes the blocks around you shoots them towards your enemies!", 350, 1500, Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK);
    }

    @Override
    protected void useAbility(PlayerEvent event) {
        Player player = event.getPlayer();

        if (!(event instanceof PlayerInteractEvent e)) {
            return;
        }
//        if (playersBlocks.containsKey(player.getUniqueId()) && (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK)) {
//            List<FallingBlock> fallingBlocks = playersBlocks.get(player.getUniqueId());
//
//            for (FallingBlock fallingBlock : fallingBlocks) {
//                fallingBlock.setVelocity(player.getLocation().getDirection().subtract(new Vector(0, 0.5, 0)));
//            }
//
//            playersBlocks.remove(player.getUniqueId());
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block lowestBlock = getLowestBlock(player.getLocation());
            List<Block> blocks = BukkitUtils.loopBlocks(lowestBlock.getLocation(), RADIUS);

            List<FallingBlock> fallingBlocks = new ArrayList<>();
            for (Block block : blocks) {
                FallingBlock fallingBlock = player.getWorld().spawnFallingBlock(block.getLocation().add(0, 1, 0), block.getBlockData());
//                fallingBlock.setGravity(false);
                fallingBlock.setDropItem(false);

//                fallingBlock.setVelocity(new Vector(0, 1, 0)); // (player.getLocation().getY() - lowestBlock.getY()) * 0.5

                fallingBlocks.add(fallingBlock);
            }
            playersBlocks.put(player.getUniqueId(), fallingBlocks);

//            long time = System.currentTimeMillis();
//
//            new BukkitRunnable() {
//                @Override
//                public void run() {
//                    if (System.currentTimeMillis() - time >= 5000) {
//                        this.cancel();
//                        return;
//                    }
//
//                    player.sendMessage(fallingBlocks.get(0).getVelocity() + "");
//                }
//            }.runTaskTimer(CaveCrawlers.getInstance(), 0, 2L);
        }
    }

    public static Block getLowestBlock(Location location) {
        for (int i = 0; i < location.getY(); i++) {
            Block block = location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY() - i, location.getBlockZ());
            if (block.getType().isSolid()) {
                return block;
            }
        }
        return location.getBlock();
    }
}
