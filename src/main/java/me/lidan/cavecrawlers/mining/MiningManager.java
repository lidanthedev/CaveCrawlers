package me.lidan.cavecrawlers.mining;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MiningManager {

    private static MiningManager instance;
    private final Map<UUID, MiningProgress> progressMap = new HashMap<>();

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
        long required = getTicksToBreak(stats.get(StatType.MINING_SPEED).getValue(), getBlockStrength(block.getType()));
        setProgress(player, new MiningProgress(player, block, required));
    }

    public long getBlockStrength(Material material) {
        if (material == Material.STONE){
            return 15;
        }
        return -1;
    }

    public static void applySlowDig(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 100000000, -1, true, false, false), true);
    }

    public static long getTicksToBreak(double miningSpeed, long blockStrength){
        return (long) (1/(miningSpeed/blockStrength/30));
    }

    public static MiningManager getInstance() {
        if (instance == null){
            instance = new MiningManager();
        }
        return instance;
    }
}
