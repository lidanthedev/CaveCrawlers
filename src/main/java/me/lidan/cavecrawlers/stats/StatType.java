package me.lidan.cavecrawlers.stats;

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum StatType {
    HEALTH("Health",
            "❤",
            ChatColor.RED, 100)
    ,DEFENSE("Defense",
            "❈",
            ChatColor.GREEN),
    INTELLIGENCE("Intelligence",
            "✎",
            ChatColor.AQUA, 100),
    DAMAGE("Damage", "❁", ChatColor.RED)
    ,STRENGTH("Strength",
            "❁",ChatColor.RED)
    ,CRIT_DAMAGE("Crit Damage",
            "☠",
            ChatColor.BLUE)
    ,CRIT_CHANCE("Crit Chance",
            "☣",
            ChatColor.BLUE)
    ,ABILITY_DAMAGE("Ability Damage",
            "๑",
            ChatColor.RED)
    ,SPEED("Speed",
            "✦",
            ChatColor.WHITE, 100)
    ,MANA("Mana",
            "✎",
            ChatColor.AQUA, 100)
    ,MINING_SPEED("Mining Speed",
            "⸕",
            ChatColor.GOLD)
    ,MINING_FORTUNE("Mining Fortune",
            "☘",
            ChatColor.GOLD);
    private final String name;
    private final String icon;
    private final ChatColor color;
    private final double base;


    StatType(String name, String icon, ChatColor color) {
        this(name, icon, color, 0.0);
    }

    StatType(String name, String icon, ChatColor color, double base) {
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.base = base;
    }

    public String getFormatName(){
        return color + icon + name;
    }

    public String getColoredName(){
        return color + name;
    }

    public static List<StatType> getStats(){
        return Arrays.asList(values());
    }

    public static List<String> names(){
        return getStats().stream().map(StatType::name).collect(Collectors.toList());
    }
}
