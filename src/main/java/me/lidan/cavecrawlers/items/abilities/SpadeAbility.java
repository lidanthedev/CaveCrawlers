package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.griffin.GriffinManager;
import me.lidan.cavecrawlers.utils.BukkitUtils;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SpadeAbility extends ClickAbility{
    GriffinManager griffinManager = GriffinManager.getInstance();

    public SpadeAbility() {
        super("Spade", "Line to gold", 20, 500, Action.LEFT_CLICK_BLOCK, Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR);
    }

    @Override
    protected void useAbility(PlayerEvent playerEvent) {
        if (playerEvent instanceof PlayerInteractEvent event){
            Player player = event.getPlayer();
            if (event.getAction() == Action.LEFT_CLICK_BLOCK){
                Block eventBlock = event.getClickedBlock();
                if (griffinManager.getGriffinBlock(player.getUniqueId()).equals(eventBlock)){
                    player.sendBlockChange(eventBlock.getLocation(), eventBlock.getBlockData());
                    griffinManager.handleGriffinBreak(player);
                }
            }
            else{
                Block block = griffinManager.getGriffinBlock(event.getPlayer().getUniqueId());
                BukkitUtils.getLineBetweenTwoPoints(player.getEyeLocation(), block.getLocation(), 1, loc -> {
                    player.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0);
                });
                player.sendBlockChange(block.getLocation(), Material.GOLD_BLOCK.createBlockData());
            }
        }
    }
}
