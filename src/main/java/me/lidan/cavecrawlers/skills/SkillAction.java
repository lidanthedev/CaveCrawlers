package me.lidan.cavecrawlers.skills;

public enum SkillAction {
    KILL, MINE, FISH, BREW;

    public static SkillAction fromString(String action) {
        return switch (action) {
            case "kill" -> KILL;
            case "mine" -> MINE;
            case "fish" -> FISH;
            case "brew" -> BREW;
            default -> valueOf(action.toUpperCase());
        };
    }
}
