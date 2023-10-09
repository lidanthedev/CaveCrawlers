package me.lidan.cavecrawlers.mining;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.shop.ShopLoader;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.utils.CustomConfig;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MiningManager {

    private static MiningManager instance;
    private final Map<Material, BlockInfo> blockInfoMap = new HashMap<>();
    private final Map<UUID, MiningProgress> progressMap = new HashMap<>();
    private final BlockInfo UNBREAKABLE_BLOCK = new BlockInfo(100000000, 10000);

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

    public MiningProgress getProgress(Player player){
        return getProgress(player.getUniqueId());
    }

    public MiningProgress getProgress(UUID player){
        return progressMap.get(player);
    }

    public void setProgress(Player player, @Nullable MiningProgress progress){
        setProgress(player.getUniqueId(), progress);
    }

    public void setProgress(UUID player, @Nullable MiningProgress progress){
        MiningProgress oldProgress = getProgress(player);
        if (oldProgress != null) {
            oldProgress.cancel();
        }
        progressMap.put(player, progress);
        if (progress != null) {
            progress.runTaskTimer(CaveCrawlers.getInstance(), 0, 1);
        }
    }

    public void breakBlock(Player player, Block block){
        applySlowDig(player);
        Stats stats = StatsManager.getInstance().getStats(player);
        double miningSpeed = stats.get(StatType.MINING_SPEED).getValue();
        double miningPower = stats.get(StatType.MINING_POWER).getValue();
        BlockInfo blockInfo = getBlockInfo(block.getType());
        if (miningPower < blockInfo.getBlockPower()){
            if (miningPower != 0 && blockInfo != UNBREAKABLE_BLOCK) {
                player.sendMessage(ChatColor.RED + "Your Mining Power is too low!");
            }
            return;
        }
        long required = getTicksToBreak(miningSpeed, blockInfo.getBlockStrength());
        setProgress(player, new MiningProgress(player, block, required));
    }

    public BlockInfo getBlockInfo(Material material) {
        return blockInfoMap.getOrDefault(material, UNBREAKABLE_BLOCK);
    }

    public CustomConfig getConfig(String ID){
        BlockLoader blockLoader = BlockLoader.getInstance();
        Map<String, File> idFileMap = blockLoader.getItemIDFileMap();
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
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, -1, -1, true, false, false), true);
    }

    public static long getTicksToBreak(double miningSpeed, int blockStrength){
        return (long) (1/(miningSpeed/blockStrength/30));
    }

    public static MiningManager getInstance() {
        if (instance == null){
            instance = new MiningManager();
        }
        return instance;
    }
}
