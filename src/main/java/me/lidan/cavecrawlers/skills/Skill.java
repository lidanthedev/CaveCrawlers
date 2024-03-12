package me.lidan.cavecrawlers.skills;

import lombok.Data;
import lombok.Setter;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import org.bukkit.entity.Player;

@Data
public class Skill {
    private SkillType type;
    private int level;
    private double xp;
    private double xpToLevel = 100;

    public Skill(SkillType type, int level) {
        this.type = type;
        this.level = level;
    }

    public void addXp(double amount){
        xp += amount;
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
}
