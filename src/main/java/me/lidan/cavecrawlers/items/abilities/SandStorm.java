package me.lidan.cavecrawlers.items.abilities;

import com.cryptomorin.xseries.particles.XParticle;
import com.google.gson.JsonObject;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.damage.AbilityDamage;
import me.lidan.cavecrawlers.stats.StatType;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class SandStorm extends ScalingClickAbility implements Listener {
    public static final String SANDSTORM_TAG = "SandStorm";
    private Material material;
    private Material fillerMaterial;
    private Particle particle;
    private Particle secondaryParticle;
    private double radius;
    private int blocks;

    // Map falling block UUID -> owning player UUID for landing damage
    private final Map<UUID, UUID> fallingBlockOwners = new HashMap<>();

    public SandStorm(double baseAbilityDamage, double abilityScaling) {
        super("Sand Storm", "Summons a circle of blocks around you, expanding outward and dealing damage!", 150, 1000, StatType.STRENGTH, baseAbilityDamage, abilityScaling);
        this.baseAbilityDamage = baseAbilityDamage;
        this.abilityScaling = abilityScaling;
        this.material = Material.SAND;
        this.fillerMaterial = Material.RED_SAND;
        this.particle = Particle.CLOUD;
        this.secondaryParticle = Particle.FLAME;
        this.statToScale = StatType.STRENGTH;
        this.radius = 2.0;
        this.blocks = 8;
    }

    @Override
    protected boolean useAbility(PlayerEvent playerEvent) {
        Player player = playerEvent.getPlayer();
        Location center = player.getLocation();
        World world = player.getWorld();
        AtomicInteger tick = new AtomicInteger(0);

        Bukkit.getScheduler().runTaskTimer(CaveCrawlers.getInstance(), bukkitTask -> {
            tick.incrementAndGet();
            double currentRadius = radius * tick.get();

            // Main circle blocks (SAND)
            for (int j = 0; j < blocks; j++) {
                double angle = 2 * Math.PI * j / blocks;
                double x = Math.cos(angle) * currentRadius;
                double z = Math.sin(angle) * currentRadius;
                Location blockLoc = center.clone().add(x, 0, z);
                summonFallingBlock(blockLoc, material, player);
            }

            // Fill air gaps with RED_SAND at a slightly offset radius
            double fillerRadius = currentRadius - (radius * 0.5);
            if (fillerRadius > 0) {
                for (int j = 0; j < blocks; j++) {
                    double angle = 2 * Math.PI * j / blocks + (Math.PI / blocks); // offset between main blocks
                    double x = Math.cos(angle) * fillerRadius;
                    double z = Math.sin(angle) * fillerRadius;
                    Location fillerLoc = center.clone().add(x, 0.5, z);
                    summonFallingBlock(fillerLoc, fillerMaterial, player);
                }
            }

            // Extra blocks at a higher Y to fill vertical air gaps (half the main points)
            int highPoints = blocks / 2;
            for (int j = 0; j < highPoints; j++) {
                double angle = 2 * Math.PI * j / highPoints;
                double x = Math.cos(angle) * currentRadius;
                double z = Math.sin(angle) * currentRadius;
                Location highLoc = center.clone().add(x, 1.0, z);
                summonFallingBlock(highLoc, fillerMaterial, player);
            }

            // Particles - cloud dust ring
            for (int p = 0; p < 20; p++) {
                double angle = 2 * Math.PI * p / 20;
                double px = Math.cos(angle) * currentRadius;
                double pz = Math.sin(angle) * currentRadius;
                Location particleLoc = center.clone().add(px, 0.5, pz);
                world.spawnParticle(particle, particleLoc, 2, 0.2, 0.2, 0.2, 0.01);
                world.spawnParticle(secondaryParticle, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);
            }

            // Center storm swirl particles
            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center.clone().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0.02);

            world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.5F, 1F);
            world.playSound(center, Sound.WEATHER_RAIN, 0.8F, 0.5F);


            if (tick.get() > 4) {
                bukkitTask.cancel();
            }
        }, 0, 3L);
        return true;
    }

    public void summonFallingBlock(Location loc, Material mat, Player owner) {
        World world = loc.getWorld();
        // Spawn particles at block origin
        world.spawnParticle(XParticle.EXPLOSION.get(), loc, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.BLOCK, loc, 6, 0.3, 0.3, 0.3, 0, Bukkit.createBlockData(mat));

        FallingBlock fallingBlock = world.spawnFallingBlock(loc, Bukkit.createBlockData(mat));
        double randomY = 0.2 + Math.random() * 0.4; // varied upward velocity
        fallingBlock.setVelocity(new Vector(0, randomY, 0));
        fallingBlock.setDropItem(false);
        fallingBlock.addScoreboardTag(SANDSTORM_TAG);

        // Track this block for landing damage
        fallingBlockOwners.put(fallingBlock.getUniqueId(), owner.getUniqueId());

        // Safety cleanup after 5 seconds in case block never lands
        Bukkit.getScheduler().runTaskLater(CaveCrawlers.getInstance(), () -> {
            fallingBlockOwners.remove(fallingBlock.getUniqueId());
        }, 100L);
    }

    /**
     * When a SandStorm falling block lands, deal damage to nearby mobs and spawn impact particles.
     */
    @EventHandler
    public void onFallingBlockLand(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock fallingBlock)) return;
        if (!fallingBlock.getScoreboardTags().contains(SANDSTORM_TAG)) return;

        event.setCancelled(true);

        UUID ownerUUID = fallingBlockOwners.remove(fallingBlock.getUniqueId());
        if (ownerUUID == null) return;

        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner == null || !owner.isOnline()) return;

        Location landLoc = fallingBlock.getLocation();
        World world = landLoc.getWorld();

        // Impact particles
        world.spawnParticle(Particle.CLOUD, landLoc, 8, 0.4, 0.2, 0.4, 0.05);
        world.spawnParticle(Particle.BLOCK, landLoc, 10, 0.3, 0.3, 0.3, 0, Bukkit.createBlockData(material));
        world.spawnParticle(Particle.FLAME, landLoc, 3, 0.2, 0.1, 0.2, 0.02);

        // Deal damage on impact
        AbilityDamage calculation = getDamageCalculation(owner);
        for (Entity entity : world.getNearbyEntities(landLoc, 2, 2, 2)) {
            if (entity instanceof Mob mob) {
                calculation.damage(owner, mob);
            }
        }
    }

    @Override
    public void abilityFailedCooldown(Player player) {
        // silent cooldown
    }

    @Override
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        SandStorm ability = (SandStorm) super.buildAbilityWithSettings(map);
        if (map.has("material")) {
            ability.material = Material.valueOf(map.get("material").getAsString());
        }
        if (map.has("fillerMaterial")) {
            ability.fillerMaterial = Material.valueOf(map.get("fillerMaterial").getAsString());
        }
        if (map.has("particle")) {
            ability.particle = Particle.valueOf(map.get("particle").getAsString());
        }
        if (map.has("secondaryParticle")) {
            ability.secondaryParticle = Particle.valueOf(map.get("secondaryParticle").getAsString());
        }
        if (map.has("radius")) {
            ability.radius = map.get("radius").getAsDouble();
        }
        if (map.has("blocks")) {
            ability.blocks = map.get("blocks").getAsInt();
        }
        return ability;
    }
}
