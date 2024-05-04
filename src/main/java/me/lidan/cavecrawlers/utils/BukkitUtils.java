package me.lidan.cavecrawlers.utils;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class BukkitUtils {
    public static Vector getVector(Entity player, double yawDegrees, double pitchDegrees, double multiplayer) {
        Vector vector = new Vector();

        double rotX = player.getLocation().getYaw() + yawDegrees;
        double rotY = player.getLocation().getPitch() + pitchDegrees;

        vector.setY(-Math.sin(Math.toRadians(rotY)));

        double xz = Math.cos(Math.toRadians(rotY));

        vector.setX(-xz * Math.sin(Math.toRadians(rotX)));
        vector.setZ(xz * Math.cos(Math.toRadians(rotX)));

        return vector.multiply(multiplayer);
    }

    public static List<Block> loopBlocks(Location center, int size) {
        ArrayList<Block> blocks = new ArrayList<>();
        int X = center.getBlockX();
        int Y = center.getBlockY();
        int Z = center.getBlockZ();
        for (int x = X - size; x <= X + size; x++) {
            for (int y = Y - size; y <= Y + size; y++) {
                for (int z = Z - size; z <= Z + size; z++) {
                    Block blockAt = center.getWorld().getBlockAt(x, y, z);
                    if (center.distanceSquared(blockAt.getLocation()) <= (double) size * size) {
                        blocks.add(blockAt);
                    }
                }
            }
        }
        return blocks;
    }

    public static boolean isSolid(Block b) {
        Material t = b.getType();
        return t.isSolid();
    }

    public static void teleportForward(Player player, double blocks) {
        int tp = 0;
        player.teleport(player.getLocation().add(0, 1, 0));
        Location l = player.getLocation().clone();
        for (int i = 0; i <= blocks; i++) {
            l.add(player.getLocation().getDirection().multiply(1)).getBlock();
            if (!isSolid(l.getBlock()) && !isSolid(l.getWorld().getBlockAt(l.getBlockX(), l.getBlockY() + 1, l.getBlockZ()))) {
                tp++;
            } else {
                player.sendMessage(ChatColor.RED + "There is a block there!");
                break;
            }
        }
        if (tp != 0) {
            l = player.getLocation().clone().add(player.getLocation().getDirection().multiply(tp));
            player.teleport(l);
        }
    }
}
