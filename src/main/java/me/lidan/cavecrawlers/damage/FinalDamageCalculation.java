package me.lidan.cavecrawlers.damage;

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
}
