package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.damage.AbilityDamage;
import me.lidan.cavecrawlers.damage.DamageManager;
import me.lidan.cavecrawlers.damage.FinalDamageCalculation;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class BoomAbility extends ClickAbility implements Listener {
    private double baseAbilityDamage;
    private double abilityScaling;

    public BoomAbility(double baseAbilityDamage, double abilityScaling) {
        super("BOOM BOOM", "Does BOOM", 0, 100);
        this.baseAbilityDamage = baseAbilityDamage;
        this.abilityScaling = abilityScaling;
    }

    @Override
    protected void useAbility(Player player) {
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
