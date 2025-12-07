package me.lidan.cavecrawlers.skills;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.stats.Stat;
import me.lidan.cavecrawlers.stats.Stats;
import org.bukkit.Material;
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
    public static final int DEFAULT_MAX_LEVEL = 50;
    public static final Material DEFAULT_ICON = Material.PAPER;
    private String id;
    private String name;
    private Map<Integer, List<SkillReward>> rewards;
    private int maxLevel;
    private boolean autoReward;
    private List<SkillObjective> objectives;
    private List<Double> xpToLevelList = new ArrayList<>();
    private Material icon;

    private final Map<SkillAction, List<SkillObjective>> actionObjectives = new HashMap<>();
    private final Map<Integer, Stats> statsRewards = new HashMap<>();

    public SkillInfo(String name, Map<Integer, List<SkillReward>> rewards, boolean autoReward, int maxLevel, List<SkillObjective> objectives, List<Double> xpToLevelList, Material icon) {
        this.name = name;
        this.rewards = rewards;
        this.autoReward = autoReward;
        this.maxLevel = maxLevel;
        this.objectives = objectives;
        this.xpToLevelList = xpToLevelList;
        this.icon = icon;
        if (autoReward) {
            generateRewards();
        }
        generateStatsRewards();
        generateActionObjectives();
    }

    public SkillInfo(String name, Map<Integer, List<SkillReward>> rewards, boolean autoReward) {
        this(name, rewards, autoReward, DEFAULT_MAX_LEVEL, new ArrayList<>(), Skill.getDefaultXpToLevelList(), DEFAULT_ICON);
    }

    public void generateActionObjectives() {
        for (SkillObjective objective : objectives) {
            SkillAction skillAction = objective.getAction();
            if (!actionObjectives.containsKey(skillAction)) {
                actionObjectives.put(skillAction, new ArrayList<>());
            }
            actionObjectives.get(skillAction).add(objective);
        }
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
        Stats stats = new Stats();
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

    public List<SkillReward> getRewards(int level) {
        return rewards.get(level);
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
        map.put("maxLevel", maxLevel);
        List<String> objectives = new ArrayList<>();
        for (SkillObjective objective : this.objectives) {
            objectives.add(objective.toSaveString());
        }
        map.put("objectives", objectives);
        map.put("xpToLevelList", xpToLevelList);
        map.put("icon", icon.name());
        return map;
    }

    public static SkillInfo deserialize(Map<String, Object> map) {
        String name = (String) map.get("name");
        Map<Integer, List<String>> rewards = (Map<Integer, List<String>>) map.get("rewards");
        Map<Integer, List<SkillReward>> rewardsMap = new HashMap<>();
        for (Map.Entry<Integer, List<String>> entry : rewards.entrySet()) {
            List<SkillReward> skillRewards = new ArrayList<>();
            for (String reward : entry.getValue()) {
                skillRewards.add(SkillReward.valueOf(reward));
            }
            rewardsMap.put(entry.getKey(), skillRewards);
        }
        boolean autoReward = (boolean) map.getOrDefault("autoReward", false);
        int maxLevel = (int) map.getOrDefault("maxLevel", DEFAULT_MAX_LEVEL);
        List<Double> xpToLevelList = (List<Double>) map.getOrDefault("xpToLevelList", Skill.getDefaultXpToLevelList());
        int xpToLevelSize = xpToLevelList.size();
        if (xpToLevelSize < maxLevel) {
            log.warn("Skill {} does not have enough xpToLevel values (has {}), filling with unreachable xp", name, xpToLevelSize);
            for (int i = xpToLevelSize; i <= maxLevel; i++) {
                xpToLevelList.add(Double.MAX_VALUE);
            }
        }
        Material icon = Material.valueOf((String) map.getOrDefault("icon", DEFAULT_ICON.name()));
        List<String> objectives = (List<String>) map.get("objectives");
        List<SkillObjective> skillObjectives = new ArrayList<>();
        if (objectives != null) {
            for (String objective : objectives) {
                skillObjectives.add(SkillObjective.valueOf(objective));
            }
        }
        return new SkillInfo(name, rewardsMap, autoReward, maxLevel, skillObjectives, xpToLevelList, icon);
    }
}
