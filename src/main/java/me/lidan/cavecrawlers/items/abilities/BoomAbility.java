package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonObject;
import lombok.ToString;
import me.lidan.cavecrawlers.damage.AbilityDamage;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;

import java.util.List;

@ToString
public class BoomAbility extends ClickAbility implements Listener {
    private double baseAbilityDamage;
    private double abilityScaling;

    public BoomAbility(double baseAbilityDamage, double abilityScaling) {
        super("BOOM BOOM", "Does BOOM", 0, 100);
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
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        BoomAbility ability = (BoomAbility) super.buildAbilityWithSettings(map);
        if (map.has("baseAbilityDamage")) {
            ability.baseAbilityDamage = map.get("baseAbilityDamage").getAsDouble();
        }
        if (map.has("abilityScaling")) {
            ability.abilityScaling = map.get("abilityScaling").getAsDouble();
        }
        return ability;
    }

    @Override
    public void abilityFailedCooldown(Player player) {
        // silent cooldown
    }
}
