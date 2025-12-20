package me.lidan.cavecrawlers.mining;

import com.cryptomorin.xseries.XAttribute;
import com.cryptomorin.xseries.XPotion;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.api.MiningAPI;
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

    public static final long HAMMER_COOLDOWN = 500;
    private static MiningManager instance;
    private final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private final Map<Material, BlockInfo> blockInfoMap = new HashMap<>();
    private final Map<UUID, MiningRunnable> progressMap = new HashMap<>();
    private final BlockInfo UNBREAKABLE_BLOCK = new BlockInfo(100000000, 10000, Map.of());
    private final Map<Block, Material> brokenBlocks = new HashMap<>();
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
    public void breakBlock(Player player, Block block){
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
            actionBarManager.showActionBar(player, ChatColor.RED + "You can't break this block with that item!");
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

    public void handleBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material originType = block.getType();
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
        handleBlockRegen(block, originType);
    }

    private void handleBlockDrops(Player player, Map<ItemInfo, Integer> drops){
        for (ItemInfo itemInfo : drops.keySet()) {
            int amount = drops.get(itemInfo);
            handleBlockDrop(player, itemInfo, amount);
        }
    }

    private void handleBlockDrop(Player player, ItemInfo itemInfo, int amount){
        Stats stats = StatsManager.getInstance().getStats(player);
        double value = stats.get(StatType.MINING_FORTUNE).getValue();
        int multi = 1 + (int) value/100;
        int remain = (int) (value % 100);
        if (RandomUtils.chanceOf(remain)){
            multi++;
        }
        amount *= multi;
        ItemsManager.getInstance().giveItem(player, itemInfo, amount);
    }

    private void handleBlockRegen(Block block, Material originType) {
        brokenBlocks.put(block, originType);
        block.setType(Material.BLACK_WOOL);

        Bukkit.getScheduler().runTaskLater(plugin, bukkitTask -> {
            block.setType(originType);
            brokenBlocks.remove(block);
        }, 100);
    }

    public void regenBlocks(){
        for (Block block : brokenBlocks.keySet()) {
            Material material = brokenBlocks.get(block);
            block.setType(material);
        }
        brokenBlocks.clear();
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
                hammerLeft -= 5;
            }
        }
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

    public static MiningManager getInstance() {
        if (instance == null) {
            instance = new MiningManager();
        }
        return instance;
    }
}
