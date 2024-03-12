package me.lidan.cavecrawlers.skills;

import lombok.Data;
import lombok.Setter;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Data
public class Skill implements ConfigurationSerializable {
    private SkillType type;
    private int level;
    private double xp;
    private double xpToLevel = 100;
    private double totalXp;

    public Skill(SkillType type, int level) {
        this.type = type;
        this.level = level;
    }

    public Skill(SkillType type, int level, double xp, double xpToLevel, double totalXp) {
        this.type = type;
        this.level = level;
        this.xp = xp;
        this.xpToLevel = xpToLevel;
        this.totalXp = totalXp;
    }

    public void addXp(double amount){
        xp += amount;
        totalXp += amount;
        while (xp >= xpToLevel && level < 50){
            level++;
            xp -= xpToLevel;
            if (xp < 0){
                xp = 0;
            }
            xpToLevel = Math.pow(level, 2) + 100; // CHANGE LATER
        }
    }

    public Stats getStats(){
        Stats stats = new Stats(true);
        for (StatType statType : type.getStatType()) {
            stats.add(statType, level);
        }
        return stats;
    }

    public void setValue(int amount) {
        level = amount;
    }

    public void add(int amount) {
        level += amount;
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        return Map.of(
                "type", type.name(),
                "level", level,
                "xp", xp,
                "xpToLevel", xpToLevel,
                "totalXp", totalXp
        );
    }

    public static Skill deserialize(Map<String, Object> map) {
        return new Skill(
                SkillType.valueOf((String) map.get("type")),
                (int) map.get("level"),
                (double) map.get("xp"),
                (double) map.get("xpToLevel"),
                (double) map.get("totalXp")
        );
    }
}
