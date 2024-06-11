package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonObject;
import lombok.ToString;
import me.lidan.cavecrawlers.damage.AbilityDamage;
import me.lidan.cavecrawlers.stats.StatType;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;

import java.util.List;

@ToString
public class BoomAbility extends ScalingClickAbility implements Listener {
    public BoomAbility(double baseAbilityDamage, double abilityScaling) {
        super("BOOM BOOM", "Does BOOM", 0, 100, StatType.INTELLIGENCE, baseAbilityDamage, abilityScaling);
        this.baseAbilityDamage = baseAbilityDamage;
        this.abilityScaling = abilityScaling;
    }

    @Override
    protected void useAbility(PlayerEvent playerEvent) {
        Player player = playerEvent.getPlayer();
        player.spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation(), 1);
        List<Entity> nearbyEntities = player.getNearbyEntities(3, 3, 3);
        AbilityDamage calculation = new AbilityDamage(player, baseAbilityDamage, abilityScaling);
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Mob mob){
                calculation.damage(player, mob);
            }
        }
    }

    @Override
    public void abilityFailedCooldown(Player player) {
        // silent cooldown
    }
}
