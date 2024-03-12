package me.lidan.cavecrawlers.skills;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.lidan.cavecrawlers.stats.Stats;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@ToString
public class Skills implements Iterable<Skill>, ConfigurationSerializable {
    private final Map<SkillType, Skill> skills;
    @Getter @Setter
    private UUID uuid;

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

    public void tryLevelUp(SkillType type){
        Skill skill = get(type);
        if (skill.levelUp()){
            Player player = Bukkit.getPlayer(uuid);
            if (player != null)
                skill.sendLevelUpMessage(player);
        }
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

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        for (SkillType statType : skills.keySet()) {
            map.put(statType.name(), get(statType));
        }
        return map;
    }

    public static Skills deserialize(Map<String, Object> map){
        Skills skills = new Skills();
        for (String key : map.keySet()) {
            if (key.equals("==")){
                continue;
            }
            Object value = map.get(key);
            SkillType type = SkillType.valueOf(key);
            Skill skill = (Skill) value;
            skills.skills.put(type, skill);
        }
        return skills;
    }

    @NotNull
    @Override
    public Iterator<Skill> iterator() {
        return skills.values().iterator();
    }
}
