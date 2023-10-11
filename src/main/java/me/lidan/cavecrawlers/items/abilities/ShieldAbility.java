package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.damage.AbilityDamage;
import me.lidan.cavecrawlers.stats.StatType;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ShieldAbility extends ClickAbility implements Listener {
    public static final String SHIELD_TAG = "Shield";
    private double baseAbilityDamage;
    private double abilityScaling;

    public ShieldAbility(double baseAbilityDamage, double abilityScaling) {
        super("Shield Throw", "Shoots snowball that deals damage scaled on defence", 0, 5000);
        this.baseAbilityDamage = baseAbilityDamage;
        this.abilityScaling = abilityScaling;
    }

    @Override
    protected void useAbility(Player player) {
        Snowball a = player.launchProjectile(Snowball.class);
        a.addScoreboardTag(SHIELD_TAG);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Snowball snowball)
            if (event.getDamager().getScoreboardTags().contains(SHIELD_TAG)) {
                if (event.getEntity() instanceof Mob mob) {
                    if (snowball.getShooter() instanceof Player player) {
                        event.setCancelled(true);
                        AbilityDamage calculation = new AbilityDamage(player, baseAbilityDamage, abilityScaling, StatType.DEFENSE,false);
                        calculation.damage(player, mob);
                    }
                }
            }
    }

    @Override
    public void abilityFailedCooldown(Player player) {
        // silent cooldown
    }

    @Override
    public List<String> toList() {
        return new ArrayList<>();
    }
}
