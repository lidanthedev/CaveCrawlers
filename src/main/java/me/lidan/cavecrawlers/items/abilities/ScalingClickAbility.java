package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonObject;
import me.lidan.cavecrawlers.damage.AbilityDamage;
import me.lidan.cavecrawlers.damage.DamageCalculation;
import me.lidan.cavecrawlers.stats.StatType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

public abstract class ScalingClickAbility extends ClickAbility{
    protected StatType statToScale;
    protected double baseAbilityDamage;
    protected double abilityScaling;
    protected boolean crit;

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
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        ScalingClickAbility ability = (ScalingClickAbility) super.buildAbilityWithSettings(map);
        if (map.has("statToScale")) {
            ability.statToScale = StatType.valueOf(map.get("statToScale").getAsString());
        }
        if (map.has("baseAbilityDamage")) {
            ability.baseAbilityDamage = map.get("baseAbilityDamage").getAsDouble();
        }
        if (map.has("abilityScaling")) {
            ability.abilityScaling = map.get("abilityScaling").getAsDouble();
        }
        return ability;
    }
}
