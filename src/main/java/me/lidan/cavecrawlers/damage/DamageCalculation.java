package me.lidan.cavecrawlers.damage;

public interface DamageCalculation {

    public double calculate();

    boolean critRoll();

    boolean isCrit();
}
