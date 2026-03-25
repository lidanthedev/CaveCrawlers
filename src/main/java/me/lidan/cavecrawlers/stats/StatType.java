package me.lidan.cavecrawlers.stats;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class StatType {
    private static final Map<String, StatType> stats = new LinkedHashMap<>();
    public static final StatType HEALTH = register("HEALTH", new StatType("Health", "❤", ChatColor.RED, 100, ChatColor.RED));
    public static final StatType DEFENSE = register("DEFENSE", new StatType("Defense", "❈", ChatColor.GREEN, 0, ChatColor.GREEN));
    public static final StatType MANA = register("MANA", new StatType("Mana", "✎", ChatColor.AQUA, 100, ChatColor.AQUA));
    public static final StatType INTELLIGENCE = register("INTELLIGENCE", new StatType("Intelligence", "✎", ChatColor.AQUA, 100, ChatColor.AQUA));
    public static final StatType MAGIC_FIND = register("MAGIC_FIND", new StatType("Magic Find", "✯", ChatColor.AQUA, 0, ChatColor.AQUA));
    public static final StatType SPEED = register("SPEED", new StatType("Speed", "✦", ChatColor.WHITE, 100, ChatColor.WHITE));
    public static final StatType DAMAGE = register("DAMAGE", new StatType("Damage", "❁", ChatColor.RED, 0, ChatColor.RED));
    public static final StatType STRENGTH = register("STRENGTH", new StatType("Strength", "❁", ChatColor.RED, 0, ChatColor.RED));
    public static final StatType CRIT_DAMAGE = register("CRIT_DAMAGE", new StatType("Crit Damage", "☠", ChatColor.BLUE, 0, ChatColor.RED));
    public static final StatType CRIT_CHANCE = register("CRIT_CHANCE", new StatType("Crit Chance", "☣", ChatColor.BLUE, 0, ChatColor.RED));
    public static final StatType ATTACK_SPEED = register("ATTACK_SPEED", new StatType("Attack Speed", "⚔", ChatColor.YELLOW, 0, ChatColor.YELLOW));
    public static final StatType ABILITY_DAMAGE = register("ABILITY_DAMAGE", new StatType("Ability Damage", "๑", ChatColor.RED, 0, ChatColor.RED));
    public static final StatType MINING_SPEED = register("MINING_SPEED", new StatType("Mining Speed", "⸕", ChatColor.GOLD, 0, ChatColor.GOLD));
    public static final StatType MINING_FORTUNE = register("MINING_FORTUNE", new StatType("Mining Fortune", "☘", ChatColor.GOLD, 0, ChatColor.GOLD));
    public static final StatType MINING_POWER = register("MINING_POWER", new StatType("Mining Power", "⸕", ChatColor.GOLD, 0, ChatColor.GOLD));
    public static final StatType MINING_HAMMER = register("MINING_HAMMER", new StatType("Mining Hammer", "⛏", ChatColor.GOLD, 0, ChatColor.GOLD));

    private String id;
    private final String name;
    private final String icon;
    private final ChatColor color;
    private final double base;
    private final ChatColor loreColor;

    public StatType(String name, String icon, ChatColor color, double base, ChatColor loreColor) {
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.base = base;
        this.loreColor = loreColor;
    }

    public static StatType valueOf(String key) {
        StatType statType = stats.get(key.toUpperCase());
        if (statType == null) {
            throw new IllegalArgumentException("Stat type " + key + " does not exist!");
        }
        return statType;
    }

    public String getFormatName(){
        return color + icon + " " + name;
    }

    public String getColoredName(){
        return color + name;
    }

    public String getColoredIcon() {
        return color + icon;
    }

    public static StatType register(String id, StatType statType) {
        id = id.toUpperCase();
        statType.id = id;
        stats.put(id, statType);
        return statType;
    }

    public static StatType[] values() {
        return stats.values().toArray(new StatType[0]);
    }

    public Component getFormatNameComponent() {
        return LegacyComponentSerializer.legacySection().deserialize(getFormatName());
    }

    public static List<StatType> getStats(){
        return Arrays.asList(values());
    }

    public static List<String> names(){
        return stats.keySet().stream().toList();
    }

    public String name() {
        return this.id;
    }
}
