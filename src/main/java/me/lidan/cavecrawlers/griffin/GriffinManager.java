package me.lidan.cavecrawlers.griffin;

import lombok.Data;
import me.lidan.cavecrawlers.utils.BukkitUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

@Data
public class GriffinManager {
    public static final int MAX_DISTANCE = 200;
    private static GriffinManager instance;
    private HashMap<UUID, Block> griffinMap = new HashMap<>();
    private World world;

    private GriffinManager() {
        world = Bukkit.getWorld("eagleisland");
    }

    public Block getGriffinBlock(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (!griffinMap.containsKey(playerUUID)) {
            griffinMap.put(playerUUID, generateGriffinLocation(player));
        }
        return griffinMap.get(playerUUID);
    }

    public void setGriffinBlock(Player player, Block location) {
        griffinMap.put(player.getUniqueId(), location);
    }

    public Block generateGriffinLocation(Player player) {
        Location pos1 = new Location(world, -88,88,148);
        Location pos2 = new Location(world, 230,88,-152);

        int distanceSquared = MAX_DISTANCE * MAX_DISTANCE;

        return BukkitUtils.getRandomBlockFilter(pos1,pos2, res -> {
            if (player.getLocation().distanceSquared(res.getLocation()) >= distanceSquared) return true;

            return res.getType() != Material.GRASS_BLOCK || res.getRelative(BlockFace.UP).getType() != Material.AIR || res.getRelative(BlockFace.UP, 2).getType() != Material.AIR;
        });
    }

    public void handleGriffinBreak(Player player){
        griffinMap.remove(player.getUniqueId());
        player.sendMessage("Griffin!!!");
    }

    public static GriffinManager getInstance() {
        if (instance == null) {
            instance = new GriffinManager();
        }
        return instance;
    }
}
