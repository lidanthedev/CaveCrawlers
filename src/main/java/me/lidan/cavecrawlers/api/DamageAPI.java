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
    /**
     * Gets the damage calculation for a player.
     *
     * @param player the player whose damage calculation is requested
     * @return the DamageCalculation for the player
     */
    DamageCalculation getDamageCalculation(Player player);

    /**
     * Sets the damage calculation for a player.
     *
     * @param player      the player whose damage calculation is to be set
     * @param calculation the DamageCalculation to set
     */
    void setDamageCalculation(Player player, DamageCalculation calculation);

    /**
     * Returns a Cooldown object that tracks attack cooldowns for mobs.
     * The returned Cooldown class can be used to check how much cooldown time remains for a specific mob.
     *
     * @param player the player whose attack cooldown is requested
     * @return the Cooldown object for the player
     */
    Cooldown<UUID> getAttackCooldown(Player player);

    /**
     * Checks if a player can attack a mob.
     *
     * @param player the player attempting to attack
     * @param mob the mob being attacked
     * @return true if the player can attack the mob, false otherwise
     */
    boolean canAttack(Player player, Mob mob);

    /**
     * Resets the attack cooldown for a player.
     *
     * @param player the player whose cooldown will be reset
     */
    void resetAttackCooldown(Player player);

    /**
     * Resets the attack cooldown for a player against a specific mob.
     *
     * @param player the player whose cooldown will be reset
     * @param mob the mob for which the cooldown will be reset
     */
    void resetAttackCooldownForMob(Player player, Mob mob);

    /**
     * Launches a projectile with a custom damage calculation and velocity.
     *
     * @param source the source of the projectile
     * @param projectile the projectile class
     * @param calculation the damage calculation to apply
     * @param velocity the velocity to launch the projectile with
     * @param <T> the type of projectile
     * @return the launched projectile
     */
    <T extends Projectile> T launchProjectile(ProjectileSource source, Class<? extends T> projectile, DamageCalculation calculation, Vector velocity);

    /**
     * Launches a projectile with a custom damage calculation.
     *
     * @param source the source of the projectile
     * @param projectile the projectile class
     * @param calculation the damage calculation to apply
     * @param <T> the type of projectile
     * @return the launched projectile
     */
    <T extends Projectile> T launchProjectile(ProjectileSource source, Class<? extends T> projectile, DamageCalculation calculation);
}
