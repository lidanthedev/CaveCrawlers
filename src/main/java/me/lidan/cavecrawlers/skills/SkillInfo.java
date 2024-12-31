package me.lidan.cavecrawlers.skills;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.stats.Stat;
import me.lidan.cavecrawlers.stats.Stats;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
@ToString
public class SkillInfo implements ConfigurationSerializable {
    private final String name;
    private final Map<Integer, List<SkillReward>> rewards;
    private final int maxLevel = 50;
    private final boolean autoReward;

    private final Map<Integer, Stats> statsRewards = new HashMap<>();

    public SkillInfo(String name, Map<Integer, List<SkillReward>> rewards, boolean autoReward) {
        this.name = name;
        this.rewards = rewards;
        this.autoReward = autoReward;
        if (autoReward) {
            generateRewards();
        }
        generateStatsRewards();
    }

    public void generateRewards() {
        for (int i = 1; i <= maxLevel; i++) {
            List<SkillReward> rewards = this.rewards.get(i);
            if (rewards == null) {
                if (autoReward) {
                    rewards = this.rewards.get(i - 1);
                    this.rewards.put(i, new ArrayList<>(rewards));
                } else
                    log.warn("Skill {} does not have rewards for level {}", name, i);
            }
        }
    }

    public void generateStatsRewards() {
        Stats stats = new Stats(true);
        for (int i = 1; i <= maxLevel; i++) {
            List<SkillReward> rewards = this.rewards.get(i);
            if (rewards == null) {
                log.warn("Skill {} does not have stats for level {}", name, i);
            } else {
                for (SkillReward reward : rewards) {
                    if (reward instanceof StatSkillReward statSkillReward) {
                        Stat stat = statSkillReward.getStat();
                        stats.add(stat.getType(), stat.getValue());
                    }
                }
            }
            statsRewards.put(i, stats.clone());
        }
    }

    public Stats getStats(int level) {
        return statsRewards.get(level);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("rewards", rewards);
        map.put("autoReward", autoReward);
        return map;
    }

    public static SkillInfo deserialize(Map<String, Object> map) {
        String name = (String) map.get("name");
        Map<Integer, List<SkillReward>> rewards = (Map<Integer, List<SkillReward>>) map.get("rewards");
        boolean autoReward = (boolean) map.get("autoReward");
        return new SkillInfo(name, rewards, autoReward);
    }
}
