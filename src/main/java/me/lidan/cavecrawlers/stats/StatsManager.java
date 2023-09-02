package me.lidan.cavecrawlers.stats;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsManager {
    private final Map<UUID, Stats> statsMap;
    private static StatsManager instance;

    public static StatsManager getInstance() {
        if (instance == null){
            instance = new StatsManager();
        }
        return instance;
    }

    public StatsManager() {
        this.statsMap = new HashMap<>();
    }

    public Stats getStats(UUID uuid){
        if (!statsMap.containsKey(uuid)){
            statsMap.put(uuid, new Stats());
        }
        return statsMap.get(uuid);
    }

    public Stats getStats(Player player){
        return getStats(player.getUniqueId());
    }
}
