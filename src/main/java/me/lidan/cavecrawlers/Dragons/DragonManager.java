package me.lidan.cavecrawlers.Dragons;

import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.items.Rarity;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class DragonManager {
    public static final World world = Bukkit.getWorld("TheEnd");
    public static final Map<Rarity, DragonDrops> DRAGON_DROPS_MAP = new HashMap<>();
    private static DragonManager instance;
    private HashMap<UUID, Block> endMap = new HashMap<>();
    private HashMap<UUID, Rarity> rarityMap = new HashMap<>();
    private final CaveCrawlers plugin;

    private DragonManager() {
        plugin = CaveCrawlers.getInstance();
    }
    public static DragonManager getInstance() {
        if (instance == null) {
            instance = new DragonManager();
        }
        return instance;
    }
    public void registerDrop(String name, DragonDrops drops) {
        DRAGON_DROPS_MAP.put(Rarity.valueOf(name), drops);
    }

    public Entity spawnMob(String mob, Location location, Player player) {
        try {
            Entity entity = plugin.getMythicBukkit().getAPIHelper().spawnMythicMob(mob, location);
            return entity;
        } catch (InvalidMobTypeException e) {
            plugin.getLogger().severe("Failed to spawn mobs");
        }
        return null;
    }
}