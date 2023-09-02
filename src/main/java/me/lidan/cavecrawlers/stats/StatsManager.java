package me.lidan.cavecrawlers.stats;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
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

    public void applyStats(Player player){
        Stats stats = getStats(player);
        player.setHealthScale(40);
        double maxHealth = stats.get(StatType.HEALTH).getValue();
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
        double regen = ((maxHealth * 0.01) + 1.5);
        double health = player.getHealth();
        player.setHealth(Math.min(health + regen, maxHealth));
        player.setFoodLevel(200);
    }

    public void statLoop(){
        Bukkit.getOnlinePlayers().forEach(this::applyStats);
    }
}
