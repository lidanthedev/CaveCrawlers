package me.lidan.cavecrawlers.griffin;

import lombok.Data;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.integration.MythicMobsHook;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.Rarity;
import me.lidan.cavecrawlers.items.abilities.SpadeAbility;
import me.lidan.cavecrawlers.utils.BukkitUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class GriffinManager {
    private static final CaveCrawlers plugin = CaveCrawlers.getInstance();
    public static final int MAX_DISTANCE = 110;
    public static final Map<Rarity, GriffinDrops> grffinDropsMap = new HashMap<>();
    public static final String WORLD_NAME = plugin.getConfig().getString("griffin-world", "world");
    public static final int DEFAULT_PROTECTION_TIME = 5000;
    private static GriffinManager instance;
    private HashMap<UUID, Block> griffinMap = new HashMap<>();
    private HashMap<UUID, Rarity> rarityMap = new HashMap<>();
    private HashMap<UUID, GriffinProtection> griffinProtectionMap = new HashMap<>();
    private World world;

    private GriffinManager() {
        world = Bukkit.getWorld(WORLD_NAME);
    }

    public void registerDrop(String name, GriffinDrops drops){
        grffinDropsMap.put(Rarity.valueOf(name), drops);
    }

    public Block getGriffinBlock(Player player) {
        UUID playerUUID = player.getUniqueId();
        ItemInfo itemInfo = ItemsManager.getInstance().getItemFromItemStackSafe(player.getInventory().getItemInMainHand());
        if (itemInfo == null){
            return null;
        }
        if (!(itemInfo.getAbility() instanceof SpadeAbility)){
            return null;
        }
        Rarity rarity = itemInfo.getRarity();
        if (rarity == null){
            return null;
        }
        if (rarity.getLevel() > rarityMap.getOrDefault(playerUUID, Rarity.COMMON).getLevel()){
            rarityMap.put(playerUUID, rarity);
        }
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
        Location pos1 = new Location(world, -75,100,95);
        Location pos2 = new Location(world, 149,64,-94);

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
        Location loc = block.getLocation().add(0,2,0);


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

    public Entity spawnMob(String mob, Location location, Player player) {
        if (plugin.getMythicBukkit() != null) {
            Entity entity = MythicMobsHook.getInstance().spawnMythicMob(mob, location);
            if (entity == null) return null;
            protectMobForPlayer(player, entity);
            return entity;
        }
        return null;
    }

    public void protectMobForPlayer(Player player, Entity entity) {
        if (entity instanceof LivingEntity livingEntity)
            griffinProtectionMap.put(entity.getUniqueId(), new GriffinProtection(System.currentTimeMillis(), (long) livingEntity.getHealth() + DEFAULT_PROTECTION_TIME, player.getUniqueId()));
    }

    public boolean isGriffinMob(Entity victim) {
        String name = ChatColor.stripColor(victim.getName());
        return name.contains("[Level ") && name.contains("]");
    }

    public int getGriffinMobLevel(String name) {
        // griffin mob name appear in this format: [Level 1] Minos Hunter
        name = ChatColor.stripColor(name);
        String[] split = name.split(" ");
        if (split.length < 2){
            return 0;
        }
        String level = split[1].split("]")[0];
        return Integer.parseInt(level);
    }
}
