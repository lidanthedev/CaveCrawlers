package me.lidan.cavecrawlers.damage;

import lombok.Getter;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class PlayerDamageCalculation implements DamageCalculation {
    private final Player player;
    private final Stats stats;
    protected boolean crit;

    public PlayerDamageCalculation(Player player) {
        this.player = player;
        this.stats = StatsManager.getInstance().getStats(player);
        this.crit = critRoll();
    }

    public double calculate(){
        double damage = stats.get(StatType.DAMAGE).getValue();
        double strength = stats.get(StatType.STRENGTH).getValue();
        double critDamage = stats.get(StatType.CRIT_DAMAGE).getValue();

        double finalDamage = (5 + damage) * (1 + strength/100);
        if (isCrit()){
            finalDamage *= 1 + critDamage / 100;
        }
        return finalDamage;
    }

    public boolean critRoll(){
        double critChance = stats.get(StatType.CRIT_CHANCE).getValue();

        // Generate random number between 0 and 100
        Random rnd = ThreadLocalRandom.current();
        int random = rnd.nextInt(100) + 1;

        // Check if random is less than crit chance
        return random <= critChance;
    }
}
