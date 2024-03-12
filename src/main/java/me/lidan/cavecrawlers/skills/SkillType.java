package me.lidan.cavecrawlers.skills;

import me.lidan.cavecrawlers.stats.StatType;
import net.md_5.bungee.api.ChatColor;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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

    public static List<SkillType> getSkills() {
        return List.of(values());
    }

    public static List<String> names() {
        return getSkills().stream().map(SkillType::name).collect(Collectors.toList());
    }

    public String getName() {
        return name;
    }

    public StatType[] getStatType() {
        return statType;
    }
}
