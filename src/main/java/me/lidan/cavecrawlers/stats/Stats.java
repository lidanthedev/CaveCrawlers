package me.lidan.cavecrawlers.stats;

import org.bukkit.Color;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Stats implements Iterable<Stat> {
    private final Map<StatType, Stat> stats;

    public Stats(List<Stat> statList) {
        this.stats = new HashMap<>();
        for (Stat stat : statList) {
            this.stats.put(stat.getType(), stat);
        }
        for (StatType type : StatType.values()) {
            if (!stats.containsKey(type)){
                stats.put(type, new Stat(type));
            }
        }
    }

    public Stats() {
        this(new ArrayList<>());
    }

    public Stat get(StatType type){
        if (stats.containsKey(type)){
            return stats.get(type);
        }
        throw new IllegalArgumentException("Stat type " + type + " Does not exist!");
    }

    public void set(StatType type, double amount){
        get(type).add(amount);
    }

    public void add(StatType type, double amount){
        get(type).add(amount);
    }

    public void remove(StatType type, double amount){
        get(type).remove(amount);
    }

    public void multiply(StatType type, double amount){
        get(type).multiply(amount);
    }

    public void multiply(double multiplier) {
        for (Stat stat : this) {
            stat.multiply(multiplier);
        }
    }

    public void add(Stats stats) {
        for (Stat stat : stats) {
            this.add(stat.getType(), stat.getValue());
        }
    }

    public List<Stat> toList() {
        return new ArrayList<>(stats.values());
    }

    public String toFormatString(){
        StringBuilder str = new StringBuilder();
        for (StatType type : StatType.getStats()) {
            Stat stat = get(type);
            str.append(stat.getType().getFormatName()).append(": ").append(stat.getValue());
            str.append("\n");
        }
        return str.toString();
    }

    public String toLoreString(){
        StringBuilder str = new StringBuilder();
        for (StatType type : StatType.getStats()) {
            Stat stat = get(type);
            if (stat.getValue() > 0){
                str.append(Color.GRAY).append(stat.getType().getName()).append(": ").append(Color.GREEN).append(stat.getValue());
                str.append("\n");
            }
        }
        return str.toString();
    }

    @Override
    public String toString() {
        return this.toList().toString();
    }

    @Override
    public @NotNull Iterator<Stat> iterator() {
        return this.toList().iterator();
    }

    @Override
    public void forEach(Consumer<? super Stat> action) {
        Objects.requireNonNull(action);
        for (Stat e : this.toList()) {
            action.accept(e);
        }
    }

    @Override
    public Spliterator<Stat> spliterator() {
        return Spliterators.spliterator(this.toList(), Spliterator.ORDERED);
    }

    public Stream<Stat> stream() {
        return StreamSupport.stream(spliterator(), false);
    }
}
