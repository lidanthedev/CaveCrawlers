package me.lidan.cavecrawlers.utils;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BukkitUtils {

    public static final int MAX_RETRIES = 100_000;

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

    public static void getLineBetweenTwoPoints(Location point1, Location point2, double space, Consumer<Location> consumer) {
        double distance = point1.distance(point2);
        Vector p1 = point1.toVector();
        Vector p2 = point2.toVector();
        Vector vector = p2.clone().subtract(p1).normalize().multiply(space);
        for (double i = 0; i < distance; i += space) {
            Location loc = p1.clone().add(vector.clone().multiply(i)).toLocation(point1.getWorld());
            consumer.accept(loc);
        }
    }

    public static void getLineWithVector(Location start, Vector vector, double space, Consumer<Location> consumer) {
        Location finish = start.clone().add(vector);
        getLineBetweenTwoPoints(start, finish, space, consumer);
    }

    public static Entity getTargetEntity(LivingEntity sender, int range) {
        Location location = sender.getEyeLocation();
        Vector vector = location.getDirection();
        World world = location.getWorld();
        for (int i = 0; i < range; i++) {
            Vector newVector = vector.clone().multiply(i);
            Location newLocation = location.clone().add(newVector);
            for (Entity entity : world.getNearbyEntities(newLocation, 0.5, 1, 0.5)) {
                if (entity != sender){
                    return entity;
                }
            }
        }
        return null;
    }

    public static Block getRandomBlockFilter(Location pos1, Location pos2, Predicate<Block> filter){
        Block block = null;
        int c = 0;
        if (pos1.getWorld() != pos2.getWorld()){
            throw new IllegalArgumentException("pos1 and pos2 World is not the Sane");
        }

        do {
            block = getRandomBlock(pos1, pos2);
            c++;
            if (c > MAX_RETRIES){
                throw new RuntimeException("Max Attempts Reached");
            }
        } while (filter.test(block));

        return block;
    }

    @NotNull
    public static Block getRandomBlock(Location pos1, Location pos2) {
        Block block;
        if (pos1.getWorld() == null){
            throw new IllegalArgumentException("world is null");
        }

        int x = new Range(pos1.getBlockX(), pos2.getBlockX()).getRandom();
        int y = new Range(pos1.getBlockY(), pos2.getBlockY()).getRandom();
        int z = new Range(pos1.getBlockZ(), pos2.getBlockZ()).getRandom();

        Location res = new Location(pos1.getWorld(), x, y, z);
        block = pos1.getWorld().getBlockAt(res);
        return block;
    }

    public static <T extends Entity> List<T> getNearbyEntities(Location target, int radius, Class<? extends Entity> clazz) {
        List<T> entities = new ArrayList<>();
        for (Entity entity : target.getWorld().getNearbyEntities(target, radius, radius, radius)) {
            if (clazz.isInstance(entity)){
                entities.add((T) entity);
            }
        }
        return entities;
    }

    public static List<LivingEntity> getNearbyEntities(Location target, int radius) {
        return getNearbyEntities(target, radius, LivingEntity.class);
    }

    public static List<Mob> getNearbyMobs(Location target, int radius) {
        return getNearbyEntities(target, radius, Mob.class);
    }
}
