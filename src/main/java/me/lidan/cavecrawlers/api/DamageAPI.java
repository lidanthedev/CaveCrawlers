package me.lidan.cavecrawlers.api;

import me.lidan.cavecrawlers.damage.DamageCalculation;
import me.lidan.cavecrawlers.utils.Cooldown;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * API for handling custom damage logic in the CaveCrawlers plugin.
 * Implementations should provide methods for applying and modifying damage.
 */
public interface DamageAPI {
    DamageCalculation getDamageCalculation(Player player);

    void setDamageCalculation(Player player, DamageCalculation calculation);

    Cooldown<UUID> getAttackCooldown(Player player);

    boolean canAttack(Player player, Mob mob);

    void resetAttackCooldown(Player player);

    void resetAttackCooldownForMob(Player player, Mob mob);

    <T extends Projectile> T launchProjectile(ProjectileSource source, Class<? extends T> projectile, DamageCalculation calculation, Vector velocity);

    <T extends Projectile> T launchProjectile(ProjectileSource source, Class<? extends T> projectile, DamageCalculation calculation);
}
