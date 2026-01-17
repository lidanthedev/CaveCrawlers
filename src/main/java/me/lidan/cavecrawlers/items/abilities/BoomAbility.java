package me.lidan.cavecrawlers.items.abilities;

import com.cryptomorin.xseries.particles.XParticle;
import lombok.ToString;
import me.lidan.cavecrawlers.damage.AbilityDamage;
import me.lidan.cavecrawlers.stats.StatType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
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
    protected boolean useAbility(PlayerEvent playerEvent) {
        Player player = playerEvent.getPlayer();
        player.spawnParticle(XParticle.EXPLOSION.get(), player.getLocation(), 1);
        List<Entity> nearbyEntities = player.getNearbyEntities(3, 3, 3);
        AbilityDamage calculation = new AbilityDamage(player, baseAbilityDamage, abilityScaling, statToScale, crit);
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Mob mob){
                calculation.damage(player, mob);
            }
        }
        return true;
    }

    @Override
    public void abilityFailedCooldown(Player player) {
        // silent cooldown
    }
}
