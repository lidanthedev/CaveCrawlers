package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonObject;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.damage.AbilityDamage;
import me.lidan.cavecrawlers.stats.StatType;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.Vector;

public class HulkAbility extends ScalingClickAbility{
    private final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private Particle particle;
    private double powerY;
    private double radius;

    public HulkAbility(StatType statToScale, double baseAbilityDamage, double abilityScaling, double powerY, double radius) {
        super("Hulk Smash", "Jump up and Smash down", 10, 1000, statToScale, baseAbilityDamage, abilityScaling);
        this.powerY = powerY;
        this.radius = radius;
        particle = Particle.EXPLOSION;
    }

    public HulkAbility() {
        this(StatType.STRENGTH, 1000, 10, 2, 5);
    }

    @Override
    protected boolean useAbility(PlayerEvent playerEvent) {
        // push the player up
        Player player = playerEvent.getPlayer();
        player.setVelocity(player.getVelocity().setY(powerY));
        // wait for the player to fall back down
        AbilityDamage calculation = new AbilityDamage(player, baseAbilityDamage, abilityScaling, statToScale,false);
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            player.setVelocity(player.getVelocity().setY(-powerY));
            if (!player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isAir()) {
                for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
                    if (entity instanceof Mob mob){
                        calculation.damage(player, mob);
                        mob.setVelocity(new Vector(0,0.5,0));
                    }
                }
                player.getWorld().spawnParticle(particle, player.getLocation(), 1, 0, 0, 0);
                task.cancel();
            }
        }, 10, 5);
        return true;
    }

    @Override
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        HulkAbility ability = (HulkAbility) super.buildAbilityWithSettings(map);
        if (map.has("statToScale")) {
            ability.statToScale = StatType.valueOf(map.get("statToScale").getAsString());
        }
        if (map.has("material")) {
        }
        if (map.has("baseAbilityDamage")) {
            ability.baseAbilityDamage = map.get("baseAbilityDamage").getAsDouble();
        }
        if (map.has("abilityScaling")) {
            ability.abilityScaling = map.get("abilityScaling").getAsDouble();
        }
        if (map.has("powerY")) {
            ability.powerY = map.get("powerY").getAsDouble();
        }
        if (map.has("radius")) {
            ability.radius = map.get("radius").getAsDouble();
        }
        if (map.has("particle")) {
            ability.particle = Particle.valueOf(map.get("particle").getAsString());
        }
        return ability;
    }
}
