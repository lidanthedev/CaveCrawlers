package me.lidan.cavecrawlers.stats;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Stat {
    private final StatType type;
    @Setter
    private double value;

    public Stat(StatType type) {
        this.type = type;
        this.value = type.getBase();
    }

    public Stat(StatType type, double value) {
        this.type = type;
        this.value = value;
    }

    public void add(double amount){
        value += amount;
    }


    public void remove(double amount){
        value -= amount;
    }

    public void multiply(double amount){
        value *= amount;
    }

    public void add(Stat stat){
        add(stat.getValue());
    }

    public void remove(Stat stat){
        remove(stat.getValue());
    }

    public void multiply(Stat stat){
        multiply(stat.getValue());
    }

    @Override
    public String toString() {
        return "Stat{" +
                "type=" + type +
                ", value=" + value +
                '}';
    }
}
