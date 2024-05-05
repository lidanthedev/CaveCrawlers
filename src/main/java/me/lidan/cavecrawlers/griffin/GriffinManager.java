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
    private static GriffinManager instance;
    private HashMap<UUID, Block> griffinMap = new HashMap<>();
    private World world;

    private GriffinManager() {
        world = Bukkit.getWorld("eagleisland");
    }

    public Block getGriffinBlock(UUID player) {
        if (!griffinMap.containsKey(player)) {
            griffinMap.put(player, generateGriffinLocation());
        }
        return griffinMap.get(player);
    }

    public void setGriffinBlock(UUID player, Block location) {
        griffinMap.put(player, location);
    }

    public Block generateGriffinLocation() {
        Location pos1 = new Location(world, -88,88,148);
        Location pos2 = new Location(world, 230,88,-152);

        return BukkitUtils.getRandomBlockFilter(pos1,pos2, res -> res.getType() != Material.GRASS_BLOCK || res.getRelative(BlockFace.UP).getType() != Material.AIR || res.getRelative(BlockFace.UP, 2).getType() != Material.AIR);
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
