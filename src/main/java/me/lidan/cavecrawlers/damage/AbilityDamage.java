package me.lidan.cavecrawlers.damage;

import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import org.bukkit.entity.Player;

public class AbilityDamage extends FinalDamageCalculation {
    private Player player;
    private Stats stats;
    private double baseAbilityDamage;
    private double abilityScaling;
    private StatType statToScale;

    public AbilityDamage(Player player, double baseAbilityDamage, double abilityScaling) {
        this(player, baseAbilityDamage, abilityScaling, StatType.INTELLIGENCE, false);
    }

    public AbilityDamage(Player player, double baseAbilityDamage, double abilityScaling, StatType statToScale, boolean crit) {
        super(0, crit);
        this.player = player;
        this.stats = StatsManager.getInstance().getStats(player);
        this.baseAbilityDamage = baseAbilityDamage;
        this.abilityScaling = abilityScaling;
        this.statToScale = statToScale;
    }

    @Override
    public double calculate() {
        double statScaled = stats.get(statToScale).getValue();
        double abilityDamage = stats.get(StatType.ABILITY_DAMAGE).getValue();
        double critDamage = stats.get(StatType.CRIT_DAMAGE).getValue();
        double finalAbilityDamage = baseAbilityDamage + abilityDamage;


        double damage = finalAbilityDamage * ((1 + statScaled/100) * abilityScaling);
        if (isCrit()){
            damage *= (1 + critDamage/100);
        }
        return damage;
    }
}
