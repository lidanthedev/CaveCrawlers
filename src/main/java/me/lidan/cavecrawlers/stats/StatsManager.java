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

        // speed
        double speed = stats.get(StatType.SPEED).getValue();
        player.setWalkSpeed((float) (speed/500));

        // health regen
        double healthRegen = ((maxHealth * 0.01) + 1.5);
        double health = player.getHealth();
        player.setHealth(Math.min(health + healthRegen, maxHealth));
        player.setFoodLevel(200);

        // mana regen
        double intel = stats.get(StatType.INTELLIGENCE).getValue();
        Stat manaStat = stats.get(StatType.MANA);
        double mana = manaStat.getValue();
        double manaRegen = intel * 0.02;
        manaStat.setValue(Math.min(mana + manaRegen, intel));
    }

    public void statLoop(){
        Bukkit.getOnlinePlayers().forEach(this::applyStats);
    }
}
