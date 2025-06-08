package me.lidan.cavecrawlers.stats;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class StatType {
    private static final Map<String, StatType> stats = new HashMap<>();

    static {
        // Register default stats
        register("HEALTH", new StatType("Health", "❤", ChatColor.RED, 100, ChatColor.RED));
        register("DEFENSE", new StatType("Defense", "❈", ChatColor.GREEN, 0, ChatColor.GREEN));
        register("MANA", new StatType("Mana", "✎", ChatColor.AQUA, 100, ChatColor.AQUA));
        register("INTELLIGENCE", new StatType("Intelligence", "✎", ChatColor.AQUA, 100, ChatColor.AQUA));
        register("MAGIC_FIND", new StatType("Magic Find", "✯", ChatColor.AQUA, 0, ChatColor.AQUA));
        register("SPEED", new StatType("Speed", "✦", ChatColor.WHITE, 100, ChatColor.WHITE));
        register("DAMAGE", new StatType("Damage", "❁", ChatColor.RED, 0, ChatColor.RED));
        register("STRENGTH", new StatType("Strength", "❁", ChatColor.RED, 0, ChatColor.RED));
        register("CRIT_DAMAGE", new StatType("Crit Damage", "☠", ChatColor.BLUE, 0, ChatColor.RED));
        register("CRIT_CHANCE", new StatType("Crit Chance", "☣", ChatColor.BLUE, 0, ChatColor.RED));
        register("ATTACK_SPEED", new StatType("Attack Speed", "⚔", ChatColor.YELLOW, 0, ChatColor.YELLOW));
        register("ABILITY_DAMAGE", new StatType("Ability Damage", "๑", ChatColor.RED, 0, ChatColor.RED));
        register("MINING_SPEED", new StatType("Mining Speed", "⸕", ChatColor.GOLD, 0, ChatColor.GOLD));
        register("MINING_FORTUNE", new StatType("Mining Fortune", "☘", ChatColor.GOLD, 0, ChatColor.GOLD));
        register("MINING_POWER", new StatType("Mining Power", "⸕", ChatColor.GOLD, 0, ChatColor.GOLD));
        register("MINING_HAMMER", new StatType("Hammer", "⛏", ChatColor.GOLD, 0, ChatColor.GOLD));
    }

    public static final StatType HEALTH = stats.get("HEALTH");
    public static final StatType DEFENSE = stats.get("DEFENSE");
    public static final StatType MANA = stats.get("MANA");
    public static final StatType INTELLIGENCE = stats.get("INTELLIGENCE");
    public static final StatType MAGIC_FIND = stats.get("MAGIC_FIND");
    public static final StatType SPEED = stats.get("SPEED");
    public static final StatType DAMAGE = stats.get("DAMAGE");
    public static final StatType STRENGTH = stats.get("STRENGTH");
    public static final StatType CRIT_DAMAGE = stats.get("CRIT_DAMAGE");
    public static final StatType CRIT_CHANCE = stats.get("CRIT_CHANCE");
    public static final StatType ATTACK_SPEED = stats.get("ATTACK_SPEED");
    public static final StatType ABILITY_DAMAGE = stats.get("ABILITY_DAMAGE");
    public static final StatType MINING_SPEED = stats.get("MINING_SPEED");
    public static final StatType MINING_FORTUNE = stats.get("MINING_FORTUNE");
    public static final StatType MINING_POWER = stats.get("MINING_POWER");
    public static final StatType MINING_HAMMER = stats.get("MINING_HAMMER");

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

    public static void register(String name, StatType statInfo) {
        stats.put(name.toUpperCase().replaceAll(" ", "_"), statInfo);
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
        return this.name.toUpperCase().replaceAll(" ", "_");
    }

    /**
     * Creates a new StatType and registers it.
     *<pre>
     * It is recommended that you declare the stat as a constant in the following format:
     *
     * public static final StatType YOUR_STAT_NAME = StatType.getByName("Your Stat Name");
     * @param name       The name of the stat type.
     * @param icon       The icon representing the stat type.
     * @param color      The color of the stat type.
     * @param base       The base value of the stat type.
     * @param loreColor  The color used in lore for this stat type.
     *                   </pre>
     */
    public static void createStatType(String name, String icon, ChatColor color, double base, ChatColor loreColor) {
        register(name.toUpperCase().replaceAll(" ", "_"), new StatType(name, icon, color, base, loreColor));
    }

    public static StatType getByName(String name) {
        return stats.get(name.toUpperCase().replaceAll(" ", "_"));
    }

    public static StatType getByKey(String key) {
        return stats.get(key);
    }
}
