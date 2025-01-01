package me.lidan.cavecrawlers.skills;

import lombok.Data;

@Data
public class SkillObjective {
    private final SkillAction action;
    private final String objective;
    private final double amount;

    public SkillObjective(SkillAction action, String objective, double amount) {
        this.action = action;
        this.objective = objective;
        this.amount = amount;
    }

    public String toSaveString() {
        return "%s %s %f".formatted(action, objective, amount);
    }

    public static SkillObjective valueOf(String objective) {
        return fromString(objective);
    }

    public static SkillObjective fromString(String objective) {
        String[] split = objective.split(" ");
        return new SkillObjective(SkillAction.fromString(split[0]), split[1], Double.parseDouble(split[2]));
    }
}
