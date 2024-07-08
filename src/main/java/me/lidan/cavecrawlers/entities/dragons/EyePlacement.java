package me.lidan.cavecrawlers.entities.dragons;

import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import me.lidan.cavecrawlers.CaveCrawlers;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EnderDragonChangePhaseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import me.lidan.cavecrawlers.items.ItemsManager;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

import static org.bukkit.Material.BEDROCK;
import static org.bukkit.Material.END_PORTAL_FRAME;

public class EyePlacement implements Listener {

    private static final Logger log = LoggerFactory.getLogger(EyePlacement.class);
    private final CaveCrawlers plugin;
    private static final int MAX_EYES = 8;
    private static final String[] DRAGON_TYPES = {"Protector", "Old", "Unstable", "Young", "Strong", "Wise", "Superior"};
    public static final World world = Bukkit.getWorld("work");
    public static final Location DRAGON_SPAWN = new Location(world, 148.14, 81.9, 2.56);
    public static final Location MIDDLE = new Location(world, 148.64, 67, 2.54);

    public EyePlacement() {
        plugin = CaveCrawlers.getInstance();
    }

    public static ArrayList<Block> loopBlocksHorizontally(Location center, double size) {
        ArrayList<Block> blocks = new ArrayList<>();
        int X = center.getBlockX();
        int Y = center.getBlockY();
        int Z = center.getBlockZ();
        for (int x = X - (int) size; x <= X + size; x++) {
            for (int y = Y - (int) size; y <= Y + size; y++) {
                for (int z = Z - (int) size; z <= Z + size; z++) {
                    if (center.distance(Objects.requireNonNull(center.getWorld()).getBlockAt(x, y, z).getLocation()) <= size) {
                        if (Math.floor(center.getY()) == (double) y) {
                            blocks.add(center.getWorld().getBlockAt(x, y, z));
                        }
                    }
                }
            }
        }
        return blocks;
    }

    public static void resetEyes() {
        double size = 10;
        ArrayList<Block> blocks = loopBlocksHorizontally(MIDDLE, size);
        for (Block block : blocks) {
            if (block.getType() == Material.BEDROCK && block.getLocation().getY() == MIDDLE.getY()) {
                block.setType(Material.END_PORTAL_FRAME);
                world.setMetadata("placed_eyes", new FixedMetadataValue(CaveCrawlers.getInstance(), 0));
            }
        }
    }
    public static void disableEyes() {
        double size = 10;
        ArrayList<Block> blocks = loopBlocksHorizontally(MIDDLE, size);
        for (Block block : blocks) {
            if (block.getType() == Material.END_PORTAL_FRAME && block.getLocation().getY() == MIDDLE.getY()) {
                block.setType(BEDROCK);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {return;}
            Player player = event.getPlayer();
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (event.getClickedBlock().getType() == Material.END_PORTAL_FRAME && event.getClickedBlock().getData() < 4) {
                String itemId = ItemsManager.getInstance().getIDofItemStackSafe(itemInHand);
                if ("SUMMONING_EYE".equals(itemId)) {
                    World world = player.getWorld();
                    int placedEyes = increasePlacedEyesCount(world);
                    Block block = event.getClickedBlock();
                    player.sendMessage(ChatColor.DARK_PURPLE + "☬ " + player.getDisplayName() + ChatColor.LIGHT_PURPLE + " placed a Summoning Eye! "+ ChatColor.GRAY + "(" + ChatColor.YELLOW + placedEyes + ChatColor.GRAY + "/" + ChatColor.GREEN + "8" + ChatColor.GRAY + ")");
                    changeBlock(block, Material.BEDROCK);
                    ItemsManager.getInstance().removeItems(player, ItemsManager.getInstance().getItemByID("SUMMONING_EYE"), 1);
                    plugin.getLogger().severe("Dragon System: Eye Placed " + player.getDisplayName() + " " + placedEyes);

                    if (placedEyes == MAX_EYES) {
                        spawnRandomDragon(world);
                    }
                }
            }
        }


    private int increasePlacedEyesCount(World world) {
        // Increment the count of placed eyes in the world's metadata or storage
        // Here, we store it in world metadata
        int placedEyes = world.getMetadata("placed_eyes").isEmpty() ? 0 : world.getMetadata("placed_eyes").get(0).asInt();
        placedEyes++;
        world.setMetadata("placed_eyes", new FixedMetadataValue(CaveCrawlers.getInstance(), placedEyes));
        return placedEyes;
    }

    private void changeBlock(Block block, Material material) {
        block.setType(material);
        EyePlacement.log.info("Changed block at " + block.getLocation() + " to " + material.toString());
    }

    private void spawnRandomDragon(World world) {
        Random random = new Random();
        String dragonType = DRAGON_TYPES[random.nextInt(DRAGON_TYPES.length)];
        spawnMob(dragonType, DRAGON_SPAWN);
        EyePlacement.log.info("Dragon Spawn: " + dragonType);
    }

    public Entity spawnMob(String mob, Location location) {
        try {
            Entity entity = plugin.getMythicBukkit().getAPIHelper().spawnMythicMob(mob, location);
            return entity;
        } catch (InvalidMobTypeException e) {
            plugin.getLogger().severe("Failed to spawn a dragon");
        }
        return null;
    }
    public static void Wait(long delay, Runnable task) {
        new BukkitRunnable() {
            @Override
            public void run() {
                task.run();
            }
        }.runTaskLater(CaveCrawlers.getInstance(), delay);
    }

    @EventHandler
    public void onDragonChangePhase(EnderDragonChangePhaseEvent e) {
        if (e.getNewPhase() == EnderDragon.Phase.DYING) {
            EyePlacement.log.info("Dragon Dying");
            e.getEntity().setHealth(0);
            e.getEntity().remove();
            Wait(80L, EyePlacement::resetEyes);
            EyePlacement.log.info("Dragon Dead");
        }
    }
}