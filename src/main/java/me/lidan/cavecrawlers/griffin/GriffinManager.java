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
import me.lidan.cavecrawlers.utils.Range;
import me.lidan.cavecrawlers.utils.VaultUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class GriffinManager {
    public static final int MAX_DISTANCE = 110;
    public static final Map<Rarity, Integer> RARITY_MOB_CHANCE = Map.of(Rarity.COMMON, 10,
            Rarity.UNCOMMON, 20,
            Rarity.RARE, 30,
            Rarity.EPIC, 40,
            Rarity.LEGENDARY, 50,
            Rarity.MYTHIC, 60);
    public static final Map<Rarity, Range> RARITY_RANGE_MAP = Map.of(Rarity.COMMON, new Range(1000, 5000),
            Rarity.UNCOMMON, new Range(5000, 10000),
            Rarity.RARE, new Range(10000, 20000),
            Rarity.EPIC, new Range(20000, 50000),
            Rarity.LEGENDARY, new Range(50000, 100000),
            Rarity.MYTHIC, new Range(60000, 110000));
    public static final Map<Rarity, GriffinDrops> grffinDropsMap = new HashMap<>();
    private static GriffinManager instance;
    private HashMap<UUID, Block> griffinMap = new HashMap<>();
    private World world;
    private final CaveCrawlers plugin;

    private GriffinManager() {
        world = Bukkit.getWorld("eagleisland");
        plugin = CaveCrawlers.getInstance();
    }

    public void registerDrop(String name, GriffinDrops drops){
        grffinDropsMap.put(Rarity.valueOf(name), drops);
        plugin.getLogger().info("Registered griffin drop for %s as %s".formatted(name, drops));
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
        Location loc = block.getLocation().add(0,2,0);
        Rarity rarity = itemInfo.getRarity();

        if (rarity == null) return;

        grffinDropsMap.get(rarity).drop(player, loc);
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

    public void spawnMob(String mob, Location location) {
        try {
            plugin.getMythicBukkit().getAPIHelper().spawnMythicMob(mob, location);
        } catch (InvalidMobTypeException e) {
            plugin.getLogger().severe("Failed to spawn mobs");
        }
    }
}
