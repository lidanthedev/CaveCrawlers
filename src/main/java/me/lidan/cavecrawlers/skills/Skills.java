package me.lidan.cavecrawlers.skills;

import lombok.Getter;
import lombok.NonNull;
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
    private final Map<String, Skill> skills;
    @Getter
    private UUID uuid;
    private transient java.util.function.BooleanSupplier mutationAllowed = () -> true;

    public void bindMutationGuard(java.util.function.BooleanSupplier guard) {
        checkMutationAllowed();
        java.util.function.BooleanSupplier previous = mutationAllowed;
        mutationAllowed = () -> previous.getAsBoolean() && guard.getAsBoolean();
        for (Skill skill : skills.values()) skill.bindMutationGuard(mutationAllowed);
    }

    public void checkMutationAllowed() {
        if (!mutationAllowed.getAsBoolean()) throw new IllegalStateException("Player persistence session is not healthy/current");
    }


    public Skills(List<Skill> skillList) {
        this.skills = new HashMap<>();
        for (Skill skill : skillList) {
            this.skills.put(skill.getType().getId(), skill);
        }
        for (SkillInfo type : SkillsManager.getInstance().getSkillInfoMap().values()) {
            if (!skills.containsKey(type.getId())) {
                skills.put(type.getId(), new Skill(type, 0));
            }
        }
    }

    public Skills() {
        this(new ArrayList<>());
    }

    public static Skills deserialize(Map<String, Object> map) {
        Skills skills = new Skills();
        for (String key : map.keySet()) {
            if (key.equals("==")) {
                continue;
            }
            Object value = map.get(key);
            SkillInfo type = SkillsManager.getInstance().getSkillInfo(key);
            if (type == null) {
                continue;
            }
            if (!(value instanceof Skill savedSkill)) {
                continue;
            }
            // Recalculate level and xp based on totalXp to ensure consistency with any changes in xp requirements
            Skill skill = new Skill(type, 0);
            if (!type.getXpToLevelList().isEmpty()) {
                skill.setXpToLevel(type.getXpToLevelList().getFirst());
            }
            skill.addXp(savedSkill.getTotalXp());
            skill.levelUp(false);
            skills.skills.put(type.getId(), skill);
        }
        return skills;
    }

    public Skill get(@NonNull SkillInfo type) {
        Skill current = skills.get(type.getId());
        if (current != null) {
            if (current.getType() != type) current.setType(type);
            return current;
        }
        return skills.computeIfAbsent(type.getId(), ignored -> {
            checkMutationAllowed();
            Skill skill = new Skill(type, 0);
            skill.setUuid(uuid);
            skill.bindMutationGuard(mutationAllowed);
            return skill;
        });
    }

    public void set(SkillInfo type, Skill skill) {
        checkMutationAllowed();
        if (skills.get(type.getId()) == skill) return;
        skill.setUuid(uuid);
        skill.bindMutationGuard(mutationAllowed);
        skills.put(type.getId(), skill);
    }

    public void addXp(SkillInfo type, double amount) {
        get(type).addXp(amount);
    }

    public void addXp(SkillInfo type, double amount, double multiplier) {
        get(type).addXp(amount * multiplier);
    }

    public void tryLevelUp(SkillInfo type) {
        checkMutationAllowed();
        Skill skill = get(type);
        int leveled = skill.levelUp(true);
        if (leveled > 0) {
            Player player = Bukkit.getPlayer(uuid);
            LevelConfigManager.getInstance().givePlayerXP(player, 10 * leveled);
            if (player != null)
                skill.sendLevelUpMessage(player);
        }
    }

    public Stats getStats() {
        Stats stats = new Stats();
        for (Skill skill : skills.values()) {
            stats.add(skill.getStats());
        }
        return stats;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
        for (Map.Entry<String, Skill> entry : skills.entrySet()) {
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
        map.putAll(skills);
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
        return Collections.unmodifiableCollection(skills.values()).iterator();
    }

    public void resetAllSkills() {
        for (Skill skill : skills.values()) {
            skill.resetSkill();
        }
    }
}
