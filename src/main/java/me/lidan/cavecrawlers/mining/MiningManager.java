package me.lidan.cavecrawlers.mining;

import com.cryptomorin.xseries.XAttribute;
import com.cryptomorin.xseries.XPotion;
import lombok.Getter;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.api.MiningAPI;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.DropsManager;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemType;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.skills.SkillAction;
import me.lidan.cavecrawlers.skills.SkillsManager;
import me.lidan.cavecrawlers.stats.*;
import me.lidan.cavecrawlers.utils.BukkitUtils;
import me.lidan.cavecrawlers.utils.Cooldown;
import me.lidan.cavecrawlers.utils.CustomConfig;
import me.lidan.cavecrawlers.utils.RandomUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MiningManager implements MiningAPI {
    private static final CaveCrawlers plugin = CaveCrawlers.getInstance();
    public static final long HAMMER_COOLDOWN = 500;
    public static final String EXPERIMENTAL_HAMMER_SORT_BY_DISTANCE = "experimental.hammer-sort-by-distance";
    public static final String EXPERIMENTAL_HAMMER_SORT_BY_FACE = "experimental.hammer-sort-by-face";
    public static final String MINING_HAMMER_PER_BLOCK_KEY = "mining.hammer-per-block";
    private static MiningManager instance;
    @Getter
    private final Map<Material, BlockInfo> blockInfoMap = new HashMap<>();
    private final Map<UUID, MiningRunnable> progressMap = new HashMap<>();
    private final Map<Block, BlockFace> lastBrokenBlockFace = new HashMap<>();
    private final BlockInfo UNBREAKABLE_BLOCK = new BlockInfo(100000000, 10000, List.of());
    private final Map<Block, BlockData> brokenBlocks = new HashMap<>();
    private final Cooldown<UUID> hammerCooldown = new Cooldown<>(HAMMER_COOLDOWN);

    @Override
    public void registerBlock(Material block, BlockInfo blockInfo){
        if (blockInfo.getBlockPower() < 0){
            blockInfoMap.remove(block);
            return;
        }
        if (blockInfo.getBlockStrength() < 0){
            blockInfoMap.remove(block);
            return;
        }
        blockInfo.setBlock(block);
        blockInfoMap.put(block, blockInfo);
    }

    @Override
    public MiningRunnable getProgress(Player player) {
        return getProgress(player.getUniqueId());
    }

    public MiningRunnable getProgress(UUID player) {
        return progressMap.get(player);
    }

    @Override
    public void setProgress(Player player, @Nullable MiningRunnable progress) {
        setProgress(player.getUniqueId(), progress);
    }

    public void setProgress(UUID player, @Nullable MiningRunnable progress) {
        MiningRunnable oldProgress = getProgress(player);
        if (oldProgress != null) {
            oldProgress.cancel();
        }
        progressMap.put(player, progress);
        if (progress != null) {
            progress.runTaskTimer(CaveCrawlers.getInstance(), 0, 1);
        }
    }

    @Override
    public void breakBlock(Player player, Block block) {
        breakBlock(player, block, BlockFace.UP);
    }

    public void breakBlock(Player player, Block block, BlockFace face) {
        lastBrokenBlockFace.put(block, face);
        applySlowDig(player);
        Stats stats = StatsManager.getInstance().getStats(player);
        double miningSpeed = stats.get(StatType.MINING_SPEED).getValue();
        double miningPower = stats.get(StatType.MINING_POWER).getValue();
        BlockInfo blockInfo = getBlockInfo(block.getType());
        ItemType brokenByBlockType = blockInfo.getBrokenBy();
        ItemInfo heldItem = ItemsManager.getInstance().getItemFromItemStack(player.getInventory().getItemInMainHand());
        if (heldItem == null){
            return;
        }
        ItemType heldItemType = heldItem.getType();
        ActionBarManager actionBarManager = ActionBarManager.getInstance();
        if (blockInfo == UNBREAKABLE_BLOCK){
            return;
        }
        if (brokenByBlockType != heldItemType){
            if (!heldItemType.isWeapon()) {
                actionBarManager.showActionBar(player, ChatColor.RED + "You can't break this block with that item!");
            }
            return;
        }
        if (miningPower < blockInfo.getBlockPower()){
            if (miningPower != 0) {
                actionBarManager.showActionBar(player, ChatColor.RED + "Your Mining Power is too low!");
            }
            return;
        }
        long required = getTicksToBreak(miningSpeed, blockInfo.getBlockStrength());
        setProgress(player, new MiningRunnable(player, block, required));
    }

    private static int getHammerPerBlock() {
        return CaveCrawlers.getInstance().getConfig().getInt(MINING_HAMMER_PER_BLOCK_KEY, 5);
    }

    private void handleBlockDrops(Player player, List<Drop> drops) {
        DropsManager.getInstance().rollDropsForPlayer(player, drops);
    }

    private void handleBlockRegen(Block block, BlockData originBlockData, BlockInfo blockInfo) {
        brokenBlocks.put(block, originBlockData);
        block.setBlockData(blockInfo.getReplacementBlockData());

        Bukkit.getScheduler().runTaskLater(plugin, bukkitTask -> {
            block.setBlockData(originBlockData);
            brokenBlocks.remove(block);
        }, 100);
    }

    public void regenBlocks(){
        for (Block block : brokenBlocks.keySet()) {
            BlockData material = brokenBlocks.get(block);
            block.setBlockData(material);
        }
        brokenBlocks.clear();
    }

    public void handleBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material originType = block.getType();
        BlockData originBlockData = block.getBlockData();
        BlockInfo blockInfo = getBlockInfo(originType);
        event.setCancelled(true);
        if (blockInfo == UNBREAKABLE_BLOCK){
            return;
        }
        player.playSound(block.getLocation(), Sound.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 1f, 1f);
        event.setDropItems(false);
        SkillsManager skillsManager = SkillsManager.getInstance();
        skillsManager.tryGiveXp(SkillAction.MINE, originType, player);
        handleBlockDrops(player, blockInfo.getDrops());
        handleHammer(player, block);
        handleBlockRegen(block, originBlockData, blockInfo);
        lastBrokenBlockFace.remove(block);
    }

    public BlockInfo getBlockInfo(Material material) {
        return blockInfoMap.getOrDefault(material, UNBREAKABLE_BLOCK);
    }

    public CustomConfig getConfig(String ID){
        BlockLoader blockLoader = BlockLoader.getInstance();
        Map<String, File> idFileMap = blockLoader.getConfigMap();
        File file = idFileMap.get(ID);
        if (file == null){
            file = new File(blockLoader.getFileDir(), ID + ".yml");
        }
        return new CustomConfig(file);
    }

    public void setBlockInfo(String ID, BlockInfo blockInfo){
        CustomConfig customConfig = getConfig(ID);
        customConfig.set(ID, blockInfo);
        customConfig.save();
        registerBlock(Material.getMaterial(ID), blockInfo);
    }

    public void clear(){
        blockInfoMap.clear();
    }

    public static void applySlowDig(Player player) {
        player.addPotionEffect(new PotionEffect(XPotion.MINING_FATIGUE.get(), -1, -1, true, false, false));
        Attribute attribute = XAttribute.BLOCK_BREAK_SPEED.get();
        if (attribute != null) {
            player.getAttribute(attribute).setBaseValue(0.0);
        }
    }

    public static long getTicksToBreak(double miningSpeed, int blockStrength){
        if (miningSpeed == 0)
            miningSpeed = 1;
        return (long) (1/(miningSpeed/blockStrength/30));
    }

    private void handleHammer(Player player, Block origin) {
        if (hammerCooldown.getCurrentCooldown(player.getUniqueId()) < HAMMER_COOLDOWN) {
            return;
        }
        hammerCooldown.startCooldown(player.getUniqueId());
        Stats stats = StatsManager.getInstance().getStats(player);
        Stat hammer = stats.get(StatType.MINING_HAMMER);
        double hammerLeft = hammer.getValue();
        int hammerSize = (int) Math.min((hammerLeft/50)+1, 6);
        List<Block> blocks = BukkitUtils.loopBlocks(origin.getLocation(), hammerSize);
        Material originType = origin.getType();
        if (plugin.getConfig().getBoolean(EXPERIMENTAL_HAMMER_SORT_BY_FACE, false)) {
            // 1. Pre-cache origin coordinates (Primitive ints are faster than Location objects)
            final int originX = origin.getX();
            final int originY = origin.getY();
            final int originZ = origin.getZ();

            // 2. Determine which axis to prioritize based on the BlockFace
            // getModX/Y/Z returns -1, 0, or 1 depending on the face direction.
            // If modY is not 0 (UP/DOWN), we prioritize the Y layer.
            BlockFace face = lastBrokenBlockFace.getOrDefault(origin, BlockFace.UP);
            final boolean checkX = face.getModX() != 0;
            final boolean checkY = face.getModY() != 0;
            final boolean checkZ = face.getModZ() != 0;

            blocks.sort((b1, b2) -> {
                // --- PRIORITY 1: The "Face" Layer ---
                // We check if the block lies on the same plane as the origin relative to the clicked face.
                boolean b1OnLayer = false;
                boolean b2OnLayer = false;

                if (checkY) {
                    b1OnLayer = (b1.getY() == originY);
                    b2OnLayer = (b2.getY() == originY);
                } else if (checkX) {
                    b1OnLayer = (b1.getX() == originX);
                    b2OnLayer = (b2.getX() == originX);
                } else if (checkZ) {
                    b1OnLayer = (b1.getZ() == originZ);
                    b2OnLayer = (b2.getZ() == originZ);
                }

                // XOR Check: If one is on the layer and the other isn't, the one on the layer wins (-1)
                if (b1OnLayer != b2OnLayer) {
                    return b1OnLayer ? -1 : 1;
                }

                // --- PRIORITY 2: Distance (Center Outward) ---
                // Optimization: Use simple multiplication instead of Math.pow() for performance
                double dx1 = b1.getX() - originX;
                double dy1 = b1.getY() - originY;
                double dz1 = b1.getZ() - originZ;
                double dist1 = (dx1 * dx1) + (dy1 * dy1) + (dz1 * dz1);

                double dx2 = b2.getX() - originX;
                double dy2 = b2.getY() - originY;
                double dz2 = b2.getZ() - originZ;
                double dist2 = (dx2 * dx2) + (dy2 * dy2) + (dz2 * dz2);

                return Double.compare(dist1, dist2);
            });
        } else if (plugin.getConfig().getBoolean(EXPERIMENTAL_HAMMER_SORT_BY_DISTANCE, false)) {
            blocks.sort((b1, b2) -> {
                double dist1 = b1.getLocation().distanceSquared(origin.getLocation());
                double dist2 = b2.getLocation().distanceSquared(origin.getLocation());
                return Double.compare(dist1, dist2);
            });
        }

        for (Block block : blocks) {
            if (block == origin) continue;
            if (block.getType() == originType){
                if (hammerLeft <= 1){
                    return;
                }
                if (RandomUtils.chanceOf(hammerLeft)){
                    player.playSound(block.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 0.1f, 1f);
                    player.breakBlock(block);
                }
                hammerLeft -= getHammerPerBlock();
            }
        }
    }

    public static MiningManager getInstance() {
        if (instance == null) {
            instance = new MiningManager();
        }
        return instance;
    }
}
