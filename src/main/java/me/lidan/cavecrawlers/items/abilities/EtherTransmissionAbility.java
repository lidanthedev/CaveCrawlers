package me.lidan.cavecrawlers.items.abilities;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;

public class EtherTransmissionAbility extends TransmissionAbility {
    public EtherTransmissionAbility(double blocks) {
        super(blocks);
    }

    @Override
    protected void useAbility(PlayerEvent event) {
        Player player = event.getPlayer();

        if (player.isSneaking()) {
            Block b = player.getTargetBlock(null, 61);
            Block b1 = b.getLocation().add(0, 1, 0).getBlock();
            Block b2 = b.getLocation().add(0, 2, 0).getBlock();
            float yaw = player.getLocation().getYaw();
            float pitch = player.getLocation().getPitch();
            Location l = b1.getLocation();
            l.add(0.5, 0, 0.5);
            l.setYaw(yaw);
            l.setPitch(pitch);
            if (b.getType() != Material.AIR) {
                if (b1.getType() == Material.AIR && b2.getType() == Material.AIR) {
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    player.teleport(l);
                    b1.getWorld().spawnParticle(Particle.PORTAL, l, 500, 0.1, 0.1, 0.1);
                } else {
                    player.sendMessage(ChatColor.RED + "There is a block there!");
                }
            } else {
                player.sendMessage(ChatColor.RED + "No blocks found!");
            }
        } else {
            super.useAbility(event);
        }
    }
}
