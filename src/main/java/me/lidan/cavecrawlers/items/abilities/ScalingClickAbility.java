package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.damage.AbilityDamage;
import me.lidan.cavecrawlers.stats.StatType;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

import java.util.List;

public abstract class ScalingClickAbility extends ClickAbility{
    @Setting
    protected StatType statToScale;
    @Setting
    protected double baseAbilityDamage;
    @Setting
    protected double abilityScaling;
    @Setting
    protected boolean crit;

    public ScalingClickAbility(String name, String description, double cost, long cooldown) {
        this(name, description, cost, cooldown, 100, 10);
    }

    public ScalingClickAbility(String name, String description, double cost, long cooldown, StatType statToScale) {
        this(name, description, cost, cooldown, statToScale, 100, 10);
    }

    public ScalingClickAbility(String name, String description, double cost, long cooldown, double baseAbilityDamage, double abilityScaling) {
        this(name, description, cost, cooldown, StatType.INTELLIGENCE, baseAbilityDamage, abilityScaling);
    }

    public ScalingClickAbility(String name, String description, double cost, long cooldown, StatType statToScale, double baseAbilityDamage, double abilityScaling) {
        this(name, description, cost, cooldown, statToScale, baseAbilityDamage, abilityScaling, Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK);
    }

    public ScalingClickAbility(String name, String description, double cost, long cooldown, StatType statToScale, double baseAbilityDamage, double abilityScaling, Action... allowedActions) {
        super(name, description, cost, cooldown, allowedActions);
        this.statToScale = statToScale;
        this.baseAbilityDamage = baseAbilityDamage;
        this.abilityScaling = abilityScaling;
        this.crit = false;
    }

    public AbilityDamage getDamageCalculation(Player player){
        return new AbilityDamage(player, baseAbilityDamage, abilityScaling, statToScale, crit);
    }

    @Override
    public List<String> toList() {
        List<String> list = super.toList();
        list.add(ChatColor.DARK_GRAY + "Scales with: " + statToScale.getFormatName());
        return list;
    }
}
