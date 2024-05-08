package me.lidan.cavecrawlers.griffin;

import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import io.lumine.mythic.bukkit.MythicBukkit;
import lombok.Data;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.Rarity;
import me.lidan.cavecrawlers.items.abilities.SpadeAbility;
import me.lidan.cavecrawlers.utils.BukkitUtils;
import me.lidan.cavecrawlers.utils.RandomUtils;
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
    public static final int MAX_DISTANCE = 100;
    private static GriffinManager instance;
    private HashMap<UUID, Block> griffinMap = new HashMap<>();
    private World world;
    private final CaveCrawlers plugin;

    private GriffinManager() {
        world = Bukkit.getWorld("eagleisland");
        plugin = CaveCrawlers.getInstance();
    }

    public Block getGriffinBlock(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (!griffinMap.containsKey(playerUUID)) {
            try{
                Block block = generateGriffinLocation(player);
                griffinMap.put(playerUUID, block);
            }
            catch (IllegalArgumentException e){
                return null;
            }
        }
        return griffinMap.get(playerUUID);
    }

    public void setGriffinBlock(Player player, Block location) {
        griffinMap.put(player.getUniqueId(), location);
    }

    public Block generateGriffinLocation(Player player) {
        return generateGriffinLocation(player, MAX_DISTANCE);
    }

    public Block generateGriffinLocation(Player player, int distance) {
        Location pos1 = new Location(world, -88,88,148);
        Location pos2 = new Location(world, 230,64,-152);

        if (player.getWorld() != world){
            throw new IllegalArgumentException("Player is not in the correct world");
        }

        int distanceSquared = distance * distance;

        return BukkitUtils.getRandomBlockFilter(pos1,pos2, res -> {
            if (player.getLocation().distanceSquared(res.getLocation()) >= distanceSquared) return true;

            return res.getType() != Material.GRASS_BLOCK || res.getRelative(BlockFace.UP).getType() != Material.AIR || res.getRelative(BlockFace.UP, 2).getType() != Material.AIR;
        });
    }

    public void handleGriffinBreak(Player player, Block block){
        griffinMap.remove(player.getUniqueId());
        ItemInfo itemInfo = ItemsManager.getInstance().getItemFromItemStackSafe(player.getInventory().getItemInMainHand());
        if (itemInfo == null){
            return;
        }
        if (!(itemInfo.getAbility() instanceof SpadeAbility)){
            return;
        }
        Rarity rarity = itemInfo.getRarity();
        if (rarity == Rarity.COMMON){
            Location loc = block.getLocation().add(0,2,0);
            try {
                if (RandomUtils.chanceOf(50)) {
                    plugin.getMythicBukkit().getAPIHelper().spawnMythicMob("MinosHunter1", loc);
                }
                else{
                    plugin.getMythicBukkit().getAPIHelper().spawnMythicMob("SiameseLynxes11", loc);
                    plugin.getMythicBukkit().getAPIHelper().spawnMythicMob("SiameseLynxes11", loc);
                }
            } catch (InvalidMobTypeException e) {
                plugin.getLogger().severe("Failed to spawn mobs");
            }
        }
    }

    public void handleGriffinClick(Player player, Block block){
        if (getGriffinBlock(player).equals(block)){
            player.sendBlockChange(block.getLocation(), block.getBlockData());
            handleGriffinBreak(player, block);
        }
    }

    public static GriffinManager getInstance() {
        if (instance == null) {
            instance = new GriffinManager();
        }
        return instance;
    }
}
