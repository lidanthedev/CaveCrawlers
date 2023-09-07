package me.lidan.cavecrawlers.items.abilities;

import lombok.Getter;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.utils.Cooldown;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
public abstract class ItemAbility {
    private final String name;
    private final String description;
    private final double cost;
    private final long cooldown;
    private final Cooldown<UUID> abilityCooldown;

    public ItemAbility(String name, String description, double cost, long cooldown) {
        this.name = name;
        this.description = description;
        this.cost = cost;
        this.cooldown = cooldown;
        this.abilityCooldown = new Cooldown<>();
    }

    public void activateAbility(Player player){
        if (abilityCooldown.getCurrentCooldown(player.getUniqueId()) < cooldown){
            abilityFailedCooldown(player);
            return;
        }
        Stats stats = StatsManager.getInstance().getStats(player);
        double mana = stats.get(StatType.MANA).getValue();
        if (mana < cost){
            abilityFailedNoMana(player);
            return;
        }

        abilityCooldown.startCooldown(player.getUniqueId());
        useAbility(player);
    }

    public void abilityFailedNoMana(Player player){

    }

    public void abilityFailedCooldown(Player player){

    }

    protected abstract void useAbility(Player player);
}
