package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonObject;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.damage.AbilityDamage;
import me.lidan.cavecrawlers.stats.StatType;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MidasAbility extends ClickAbility implements Listener {
    public static final String SHIELD_TAG = "Shield";
    private StatType statToScale;
    private Material material;
    private double baseAbilityDamage;
    private double abilityScaling;

    public MidasAbility(double baseAbilityDamage, double abilityScaling) {
        super("Blocks Throw", "Cast a wave of blocks in the direction you are facing! Dealing damage.", 0, 10);
        this.baseAbilityDamage = baseAbilityDamage;
        this.abilityScaling = abilityScaling;
        material = Material.GOLD_BLOCK;
        statToScale = StatType.DEFENSE;
    }

    @Override
    protected void useAbility(PlayerEvent playerEvent) {
        Player player = playerEvent.getPlayer();
        Location loc =  player.getLocation();
        Vector vector = loc.clone().getDirection();
        World world = player.getWorld();
        vector.setY(0);
        vector.normalize();
        loc = loc.add(vector);
        AtomicInteger i = new AtomicInteger(0);
        Location locS = loc;
        List<Mob> hitEntityList = new ArrayList<>();
        Bukkit.getScheduler().runTaskTimer(CaveCrawlers.getInstance(), bukkitTask -> {
            i.set(i.get()+1);
            Vector newVector = vector.clone().multiply(i.get());
            Location newLoc = locS.clone().add(newVector);
            Location newLoc2 = newLoc.clone();
            Location newLoc3 = newLoc.clone();
            newLoc2.setYaw(newLoc2.getYaw()+90);
            newLoc3.setYaw(newLoc3.getYaw()-90);
            Vector vector2 = newLoc2.getDirection().multiply(1);
            Vector vector3 = newLoc3.getDirection().multiply(1);
            vector2.setY(0);
            vector2.normalize();
            vector3.setY(0);
            vector3.normalize();
            newLoc2 = newLoc.clone().add(vector2);
            newLoc3 = newLoc.clone().add(vector3);
            summonFallingBlock(newLoc);
            summonFallingBlock(newLoc2);
            summonFallingBlock(newLoc3);
            world.playSound(newLoc,Sound.ENTITY_GENERIC_EXPLODE,0.5F,1F);
            for (Entity entity : world.getNearbyEntities(newLoc, 5, 3, 5)) {
                if (entity instanceof Mob mob){
                    if (!hitEntityList.contains(mob)) {
                        hitEntityList.add(mob);
                        AbilityDamage calculation = new AbilityDamage(player, baseAbilityDamage, abilityScaling, statToScale,false);
                        calculation.damage(player, mob);
                        mob.setVelocity(new Vector(0,0.5,0));
                    }
                }
            }
            if (i.get() > 10) {
                bukkitTask.cancel();
            }
        }, 0, 3L);
    }

    public void summonFallingBlock(Location loc) {
        World world = loc.getWorld();
        world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0,0,0,0);
        FallingBlock block = world.spawnFallingBlock(loc, material, (byte) 0);
        block.setVelocity(new Vector(0,0.3,0));
        block.setDropItem(false);
    }

    @Override
    public void abilityFailedCooldown(Player player) {
        // silent cooldown
    }

    @Override
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        MidasAbility ability = (MidasAbility) super.buildAbilityWithSettings(map);
        if (map.has("baseAbilityDamage")){
            ability.baseAbilityDamage = map.get("baseAbilityDamage").getAsDouble();
        }
        if (map.has("abilityScaling")){
            ability.abilityScaling = map.get("abilityScaling").getAsDouble();
        }
        if (map.has("material")){
            ability.material = Material.valueOf(map.get("material").getAsString());
        }
        if (map.has("statToScale")){
            ability.statToScale = StatType.valueOf(map.get("statToScale").getAsString());
        }
        return ability;
    }
}
