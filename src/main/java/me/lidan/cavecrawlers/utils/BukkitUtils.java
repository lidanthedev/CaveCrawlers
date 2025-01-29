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

    /**
     * Get the vector of the player
     *
     * @param player       the player
     * @param yawDegrees   the yaw degrees
     * @param pitchDegrees the pitch degrees
     * @param multiplayer  the multiplayer
     * @return the vector rotated by the yaw and pitch
     */
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

    /**
     * Get blocks in a radius
     * @param center the center location of the blocks
     * @param radius the radius around the center
     * @return the blocks in the radius
     */
    public static List<Block> loopBlocks(Location center, int radius) {
        ArrayList<Block> blocks = new ArrayList<>();
        int X = center.getBlockX();
        int Y = center.getBlockY();
        int Z = center.getBlockZ();
        for (int x = X - radius; x <= X + radius; x++) {
            for (int y = Y - radius; y <= Y + radius; y++) {
                for (int z = Z - radius; z <= Z + radius; z++) {
                    Block blockAt = center.getWorld().getBlockAt(x, y, z);
                    if (center.distanceSquared(blockAt.getLocation()) <= (double) radius * radius) {
                        blocks.add(blockAt);
                    }
                }
            }
        }
        return blocks;
    }

    /**
     * Check if a block is solid
     * @param block the block
     * @return true if the block is solid
     */
    public static boolean isSolid(Block block) {
        Material type = block.getType();
        return type.isSolid();
    }

    /**
     * Teleport the player forward by a number of blocks with safeguards
     * @param player the player
     * @param blocks the number of blocks
     */
    public static void teleportForward(Player player, double blocks) {
        int tp = 0;
        player.teleport(player.getLocation().add(0, 1, 0));
        Location l = player.getLocation().clone();
        for (int i = 0; i <= blocks; i++) {
            l.add(player.getLocation().getDirection().multiply(1)).getBlock();
            if (!isSolid(l.getBlock()) && !isSolid(player.getWorld().getBlockAt(l.getBlockX(), l.getBlockY() + 1, l.getBlockZ()))) {
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

    /**
     * Run a callback every jump between two points
     * @param point1 the first point
     * @param point2 the second point
     * @param space the space between the jumps
     * @param callback action to perform on every point in the line
     */
    public static void runCallbackBetweenTwoPoints(Location point1, Location point2, double space, Consumer<Location> callback) {
        double distance = point1.distance(point2);
        Vector p1 = point1.toVector();
        Vector p2 = point2.toVector();
        Vector vector = p2.clone().subtract(p1).normalize().multiply(space);
        for (double i = 0; i < distance; i += space) {
            Location loc = p1.clone().add(vector.clone().multiply(i)).toLocation(point1.getWorld());
            callback.accept(loc);
        }
    }

    /**
     * Run a callback every jump between point with a vector
     * @param start the start
     * @param vector the vector
     * @param space the space between the jumps
     * @param consumer the action to perform on every point in the line
     */
    public static void runCallbackBetweenPointWithVector(Location start, Vector vector, double space, Consumer<Location> consumer) {
        Location finish = start.clone().add(vector);
        runCallbackBetweenTwoPoints(start, finish, space, consumer);
    }

    /**
     * Get the target entity
     * @param sender the sender entity
     * @param range max range
     * @return the target entity
     */
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

    /**
     * Get random block in a region
     * @param pos1 the first position
     * @param pos2 the second position
     * @param filter what blocks to filter
     * @return the random block in the region
     */
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

    /**
     * Get random block in a region
     * @param pos1 the first position
     * @param pos2 the second position
     * @return the random block in the region
     */
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

    /**
     * Get nearby entities around a location with a filter
     * @param center the center location
     * @param radius the radius
     * @param entityFilter the entity class to cast into (Ex: LivingEntity.class)
     * @return the nearby entities
     * @param <T> the type of entity
     */
    public static <T extends Entity> List<T> getNearbyEntities(Location center, int radius, Class<? extends T> entityFilter) {
        List<T> entities = new ArrayList<>();
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entityFilter.isInstance(entity)) {
                entities.add(entityFilter.cast(entity));
            }
        }
        return entities;
    }

    /**
     * Get nearby living entities around a location
     * @param center the center location
     * @param radius the radius
     * @return the nearby living entities around the center within radius distance
     */
    public static List<LivingEntity> getNearbyEntities(Location center, int radius) {
        return getNearbyEntities(center, radius, LivingEntity.class);
    }

    /**
     * Get nearby mobs around a location
     * @param center the center location
     * @param radius the radius
     * @return the nearby mobs around the center within radius distance
     */
    public static List<Mob> getNearbyMobs(Location center, int radius) {
        return getNearbyEntities(center, radius, Mob.class);
    }
}
