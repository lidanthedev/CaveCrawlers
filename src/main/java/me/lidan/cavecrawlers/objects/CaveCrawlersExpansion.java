package me.lidan.cavecrawlers.objects;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.StatsManager;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CaveCrawlersExpansion extends PlaceholderExpansion {

    private final StatsManager statsManager;
    private CaveCrawlers plugin;

    public CaveCrawlersExpansion(CaveCrawlers plugin) {
        this.plugin = plugin;
        statsManager = StatsManager.getInstance();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "cavecrawlers";
    }

    @Override
    public @NotNull String getAuthor() {
        return "LidanTheGamer";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String[] args = params.split("_");
        if (args[0].equalsIgnoreCase("stat")){
            plugin.getLogger().info("Placeholder Stat");
            if (args.length == 2){
                plugin.getLogger().info("Getting stat " + args[1] + " for " + player.getName());
                StatType statType = StatType.valueOf(args[1]);
                return String.valueOf(statsManager.getStats(player.getUniqueId()).get(statType).getValue());
            }
        }
        return null;
    }
}
