package me.lidan.cavecrawlers.stats;

import org.bukkit.Color;

public enum StatType {
    DAMAGE("Damage", "❁", Color.RED)
    ,STRENGTH("Strength",
            "❁",Color.RED)
    ,CRIT_DAMAGE("Crit Damage",
            "☠",
            Color.BLUE)
    ,CRIT_CHANCE("Crit Chance",
            "☣",
            Color.BLUE)
    ,ABILITY_DAMAGE("Ability Damage",
            "๑",
            Color.RED)
    ,HEALTH("Health",
            "❤",
            Color.RED)
    ,DEFENSE("Defense",
            "❈",
            Color.GREEN)
    ,SPEED("Speed",
            "✦",
            Color.WHITE, 100)
    ,INTELLIGENCE("Intelligence",
            "✎",
            Color.AQUA, 100)
    ,MANA("Mana",
            "✎",
            Color.AQUA, 100)
    ,MINING_SPEED("Mining Speed",
            "⸕",
            Color.ORANGE)
    ,MINING_FORTUNE("Mining Fortune",
            "☘",
            Color.ORANGE);
    private final String name;
    private final String icon;
    private final Color color;
    private final double base;

    StatType(String name, String icon, Color color) {
        this(name, icon, color, 0.0);
    }

    StatType(String name, String icon, Color color, double base) {
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.base = base;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public Color getColor() {
        return color;
    }

    public double getBase() {
        return base;
    }

    public String getFormatName(){
        return color + name + icon;
    }

    public String getColoredName(){
        return color + name;
    }
}
