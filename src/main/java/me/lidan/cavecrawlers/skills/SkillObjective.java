package me.lidan.cavecrawlers.skills;

import lombok.Data;

import java.util.List;

/**
 * Represents a skill objective, which is a task that the player must complete to gain experience in a skill.
 * Format: [action] [objective] [amount] [worlds]
 * Example: KILL ZOMBIE 10 world1,world2
 */
@Data
public class SkillObjective {
    private final SkillAction action;
    private final String objective;
    private final double amount;
    private final List<String> worlds;

    public SkillObjective(SkillAction action, String objective, double amount, List<String> worlds) {
        this.action = action;
        this.objective = objective;
        this.amount = amount;
        this.worlds = worlds;
    }

    public String toSaveString() {
        return "%s %s %f %s".formatted(action, objective, amount, String.join(",", worlds));
    }

    public static SkillObjective valueOf(String skillObjectiveString) {
        return fromString(skillObjectiveString);
    }

    public static SkillObjective fromString(String skillObjectiveString) throws IllegalArgumentException {
        String[] split = skillObjectiveString.split(" ");
        if (split.length < 2) {
            throw new IllegalArgumentException("Invalid skill objective string: " + skillObjectiveString);
        }
        List<String> worlds = List.of();
        if (split.length > 3) {
            worlds = List.of(split[3].split(","));
        }
        return new SkillObjective(SkillAction.fromString(split[0]), split[1], Double.parseDouble(split[2]), worlds);
    }
}
