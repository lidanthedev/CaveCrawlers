package me.lidan.cavecrawlers.skills;

import lombok.Data;
import me.lidan.cavecrawlers.stats.Stats;

@Data
public class SkillInfo {
    private final String name;
    private final Stats[] rewards;

    public SkillInfo(String name, Stats[] rewards) {
        this.name = name;
        this.rewards = rewards;
    }
}
