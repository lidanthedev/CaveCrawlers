package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.damage.AbilityDamage;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class LaserAbility extends ItemAbility implements Listener {
    private Particle particle;
    private double baseAbilityDamage;
    private double abilityScaling;
    private int range;

    public LaserAbility(String name, String description, double cost, long cooldown, Particle particle, double baseAbilityDamage, double abilityScaling, int range) {
        super(name, description, cost, cooldown);
        this.particle = particle;
        this.baseAbilityDamage = baseAbilityDamage;
        this.abilityScaling = abilityScaling;
        this.range = range;
    }

    @Override
    protected void useAbility(Player player) {
        Location location = player.getEyeLocation();
        Vector vector = location.getDirection();
        World world = location.getWorld();
        AbilityDamage abilityDamage = new AbilityDamage(player, baseAbilityDamage, abilityScaling);
        List<Mob> hitEntityList = new ArrayList<>();
        for (int i = 0; i < range; i++) {
            Vector newVector = vector.clone().multiply(i);
            Location newLocation = location.clone().add(newVector);
            for (Entity entity : world.getNearbyEntities(newLocation, 0.5, 1, 0.5)) {
                if (entity instanceof Mob mob){
                    if (!hitEntityList.contains(mob)) {
                        hitEntityList.add(mob);
                        abilityDamage.damage(player, mob);
                    }
                }
            }
            world.spawnParticle(particle, newLocation, 1, 0,0,0,0);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hasAbility(hand)){
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK){
                activateAbility(player);
            }
        }
    }

    @Override
    public void abilityFailedCooldown(Player player) {
        // silent cooldown
    }
}
