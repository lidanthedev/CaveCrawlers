package me.lidan.cavecrawlers.stats;

import me.lidan.cavecrawlers.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Stats implements Iterable<Stat>, ConfigurationSerializable, Cloneable {
    private Map<StatType, Stat> stats;

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
        if (!stats.containsKey(type)) {
            stats.put(type, new Stat(type));
        }
        return stats.get(type);
    }

    public void set(StatType type, double amount){
        get(type).setValue(amount);
    }

    public void add(StatType type, double amount){
        if (type == StatType.MANA){
            type = StatType.INTELLIGENCE;
        }
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

    public void reset(){
        for (Stat stat : this) {
            stat.setValue(stat.getType().getBase());
        }
    }

    public void zero(){
        for (Stat stat : this) {
            stat.setValue(0);
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
            str.append(stat.getType().getFormatName()).append(": ").append(Math.round(stat.getValue() * 100.0f) / 100.0f);
            str.append("\n");
        }
        return str.toString();
    }

    public List<String> toLoreList(){
        List<String> lore = new ArrayList<>();
        for (StatType type : StatType.getStats()) {
            Stat stat = get(type);
            double value = stat.getValue();
            if (value > 0){
                String numberWithoutDot = StringUtils.getNumberWithoutDot(value);
                lore.add(ChatColor.GRAY + stat.getType().getName() + ": " + type.getLoreColor() + "+" + numberWithoutDot);
            }
        }
        return lore;
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

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        for (StatType statType : stats.keySet()) {
            map.put(statType.name(), get(statType).getValue());
        }
        return map;
    }

    public static Stats deserialize(Map<String, Object> map){
        Stats stats = new Stats();
        for (String key : map.keySet()) {
            StatType type = StatType.valueOf(key);
            Double value = (Double) map.get(key);
            stats.set(type, value);
        }
        return stats;
    }

    @Override
    public Stats clone() {
        try {
            Stats clone = (Stats) super.clone();
            clone.stats = new HashMap<>();
            for (Stat stat : this) {
                clone.stats.put(stat.getType(), new Stat(stat.getType(), stat.getValue()));
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
