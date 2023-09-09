package me.lidan.cavecrawlers.damage;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public class FinalDamageCalculation implements DamageCalculation{

    private double damage;

    private boolean crit;

    public FinalDamageCalculation(double damage, boolean crit) {
        this.damage = damage;
        this.crit = crit;
    }


    @Override
    public double calculate() {
        return damage;
    }

    @Override
    public boolean critRoll() {
        return crit;
    }

    @Override
    public boolean isCrit() {
        return crit;
    }

    public void damage(Player player, Mob mob){
        DamageManager damageManager = DamageManager.getInstance();
        damageManager.setDamageCalculation(player, this);
        damageManager.resetAttackCooldownForMob(player, mob);
        mob.damage(damage, player);
    }
}
