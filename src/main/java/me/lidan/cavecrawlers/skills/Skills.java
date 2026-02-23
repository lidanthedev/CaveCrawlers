package me.lidan.cavecrawlers.skills;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import me.lidan.cavecrawlers.levels.LevelConfigManager;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@ToString
public class Skills implements Iterable<Skill>, ConfigurationSerializable {
    private final Map<SkillInfo, Skill> skills;
    @Getter @Setter
    private UUID uuid;

    public Skills(List<Skill> skillList){
        this.skills = new HashMap<>();
        for (Skill skill : skillList) {
            this.skills.put(skill.getType(), skill);
        }
        for (SkillInfo type : SkillsManager.getInstance().getSkillInfoMap().values()) {
            if (!skills.containsKey(type)) {
                skills.put(type, new Skill(type, 0));
            }
        }
    }

    public Skills() {
        this(new ArrayList<>());
    }

    public Skill get(@NonNull SkillInfo type) {
        return skills.computeIfAbsent(type, t -> new Skill(t, 0));
    }

    public void set(SkillInfo type, Skill skill) {
        skills.put(type, skill);
    }

    public void addXp(SkillInfo type, double amount) {
        get(type).addXp(amount);
    }

    public void addXp(SkillInfo type, double amount, double multiplier) {
        get(type).addXp(amount * multiplier);
    }

    public void tryLevelUp(SkillInfo type) {
        Skill skill = get(type);
        int leveled = skill.levelUp(true);
        if (leveled > 0) {
            Player player = Bukkit.getPlayer(uuid);
            LevelConfigManager.getInstance().givePlayerXP(player, 10 * leveled);
            if (player != null)
                skill.sendLevelUpMessage(player);
        }
    }

    public Stats getStats(){
        Stats stats = new Stats();
        for (Skill skill : skills.values()) {
            stats.add(skill.getStats());
        }
        return stats;
    }

    public static Skills deserialize(Map<String, Object> map){
        Skills skills = new Skills();
        for (String key : map.keySet()) {
            if (key.equals("==")){
                continue;
            }
            Object value = map.get(key);
            SkillInfo type = SkillsManager.getInstance().getSkillInfo(key);
            if (type == null) {
                continue;
            }
            Skill savedSkill = (Skill) value;
            // Recalculate level and xp based on totalXp to ensure consistency with any changes in xp requirements
            Skill skill = new Skill(type, 0);
            skill.addXp(savedSkill.getTotalXp());
            skill.levelUp(false);
            skills.skills.put(type, skill);
        }
        return skills;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
        for (Map.Entry<SkillInfo, Skill> entry : skills.entrySet()) {
            Skill skill = entry.getValue();
            if (skill != null) {
                skill.setUuid(uuid);
            }
        }
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        for (SkillInfo skillInfo : skills.keySet()) {
            map.put(skillInfo.getId(), get(skillInfo));
        }
        return map;
    }

    public String toFormatString() {
        StringBuilder builder = new StringBuilder();
        for (Skill skill : skills.values()) {
            builder.append(skill.getType().getName()).append(": ").append(skill.getLevel()).append(" xp: ").append(StringUtils.getNumberFormat(skill.getXp())).append("/").append(StringUtils.getNumberFormat(skill.getXpToLevel())).append(" total: ").append(StringUtils.getNumberFormat(skill.getTotalXp())).append("\n");
        }
        return builder.toString();
    }

    @NotNull
    @Override
    public Iterator<Skill> iterator() {
        return skills.values().iterator();
    }

    public void resetAllSkills() {
        for (Skill skill : skills.values()) {
            skill.resetSkill();
        }
    }
}
