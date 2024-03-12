package me.lidan.cavecrawlers.skills;

import me.lidan.cavecrawlers.stats.Stat;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Skills {
    private final Map<SkillType, Skill> skills;

    public Skills(List<Skill> skillList){
        this.skills = new HashMap<>();
        for (Skill skill : skillList) {
            this.skills.put(skill.getType(), skill);
        }
        for (SkillType type : SkillType.values()) {
            if (!skills.containsKey(type)){
                skills.put(type, new Skill(type, 0));
            }
        }
    }

    public Skills() {
        this(new ArrayList<>());
    }

    public Skill get(SkillType type){
        return skills.get(type);
    }

    public void addXp(SkillType type, double amount){
        get(type).addXp(amount);
    }

    public void addXp(SkillType type, double amount, double multiplier){
        get(type).addXp(amount * multiplier);
    }

    public Stats getStats(){
        Stats stats = new Stats(true);
        for (Skill skill : skills.values()) {
            stats.add(skill.getStats());
        }
        return stats;
    }

    public String toFormatString() {
        StringBuilder builder = new StringBuilder();
        for (Skill skill : skills.values()) {
            builder.append(skill.getType()).append(": ").append(skill.getLevel()).append(" xp: ").append(skill.getXp()).append("/").append(skill.getXpToLevel()).append("\n");
        }
        return builder.toString();
    }
}
