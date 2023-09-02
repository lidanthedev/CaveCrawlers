package me.lidan.cavecrawlers.stats;

public class Stat {
    private StatType type;
    private double value;

    public Stat(StatType type) {
        this.type = type;
        this.value = type.getBase();
    }

    public Stat(StatType type, double value) {
        this.type = type;
        this.value = value;
    }


}
