package me.lidan.cavecrawlers.items.abilities;

import lombok.ToString;
import me.lidan.cavecrawlers.damage.AbilityDamage;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.utils.BukkitUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

@ToString
public class SoulreaperAbility extends ScalingClickAbility implements Listener {
    private double blocks;
    public SoulreaperAbility(double baseAbilityDamage, double abilityScaling, double blocks) {
        super("Reaper Impact",  "§fTeleport §a10 blocks §fahead §fof you. §fimplode dealing §c10,000 §c❁ §cDamage §fto nearby enemies §fand granting an §6❤ Absorption §fshield for §e5 §fseconds.", 250, 100, StatType.INTELLIGENCE, baseAbilityDamage, abilityScaling);
        this.baseAbilityDamage = baseAbilityDamage;
        this.abilityScaling = abilityScaling;
        this.blocks = blocks;
    }

    @Override
    protected void useAbility(PlayerEvent playerEvent) {
        Player player = playerEvent.getPlayer();
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        BukkitUtils.teleportForward(player, blocks);
        player.spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation(), 1);
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 5));
//        player.sendMessage("ERROR Absorption");
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

