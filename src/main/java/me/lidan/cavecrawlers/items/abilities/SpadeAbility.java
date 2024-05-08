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
        super("Spade", "Line to gold", 20, 500);
    }

    @Override
    protected void useAbility(PlayerEvent playerEvent) {
        if (playerEvent instanceof PlayerInteractEvent event){
            Player player = event.getPlayer();
            Block block = griffinManager.getGriffinBlock(event.getPlayer());
            BukkitUtils.getLineBetweenTwoPoints(player.getEyeLocation(), block.getLocation(), 1, loc -> {
                player.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0);
            });
            player.sendBlockChange(block.getLocation(), Material.GOLD_BLOCK.createBlockData());
        }
    }
}
