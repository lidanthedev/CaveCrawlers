package me.lidan.cavecrawlers.damage;

import lombok.Getter;
import me.lidan.cavecrawlers.stats.Stat;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.utils.Cooldown;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DamageManager {

    public static final int BASE_ATTACK_COOLDOWN = 500;
    public static final float MINIMUM_ATTACK_COOLDOWN = 250f;
    private static DamageManager instance;

    @Getter
    private final Map<UUID, Cooldown<UUID>> attackMap = new HashMap<>();

    public Cooldown<UUID> getAttackCooldown(Player player){
        if (!attackMap.containsKey(player.getUniqueId())){
            attackMap.put(player.getUniqueId(), new Cooldown<>());
        }
        return attackMap.get(player.getUniqueId());
    }

    public boolean canAttack(Player player, Mob mob){
        Stats stats = StatsManager.getInstance().getStats(player);
        Stat attackSpeedStat = stats.get(StatType.ATTACK_SPEED);
        Cooldown<UUID> cooldown = getAttackCooldown(player);
        long attackCooldown = calculateAttackSpeed((long) attackSpeedStat.getValue());
        if (cooldown.getCurrentCooldown(mob.getUniqueId()) < attackCooldown){
            return false;
        }
        cooldown.startCooldown(mob.getUniqueId());
        return true;
    }

    public void resetAttackCooldown(Player player){
        attackMap.remove(player.getUniqueId());
    }

    public void resetAttackCooldownForMob(Player player, Mob mob){
        Cooldown<UUID> cooldown = getAttackCooldown(player);
        cooldown.setCooldown(mob.getUniqueId(), 0L);
    }

    public long calculateAttackSpeed(long attackSpeed){
        return (long) (BASE_ATTACK_COOLDOWN - (MINIMUM_ATTACK_COOLDOWN / 100 * attackSpeed));
    }

    public static DamageManager getInstance() {
        if (instance == null){
            instance = new DamageManager();
        }
        return instance;
    }
}
