package me.lidan.cavecrawlers.skills;

import me.lidan.cavecrawlers.stats.StatType;
import net.md_5.bungee.api.ChatColor;

public enum SkillType {
    COMBAT("Combat", new StatType[] {StatType.CRIT_CHANCE}),
    MINING("Mining", new StatType[] {StatType.DEFENSE}),
    FARMING("Farming", new StatType[] {StatType.HEALTH}),
    FORAGING("Foraging", new StatType[] {StatType.STRENGTH}),
    FISHING("Fishing", new StatType[] {StatType.HEALTH}),
    ALCHEMY("Alchemy", new StatType[] {StatType.INTELLIGENCE}),

    ;
    private final String name;
    private final StatType[] statType;

    SkillType(String name, StatType[] statType) {
        this.name = name;
        this.statType = statType;
    }

    public String getName() {
        return name;
    }

    public StatType[] getStatType() {
        return statType;
    }
}
