package me.lidan.cavecrawlers.storage.db;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SkillRow {
    private String playerUuid;
    private String type;
    private double xp;
    private int level;
    private double totalXp;

    public SkillRow(String playerUuid, String type, double xp, int level, double totalXp) {
        this.playerUuid = playerUuid;
        this.type = type;
        this.xp = xp;
        this.level = level;
        this.totalXp = totalXp;
    }

    public String getType() {
        return type;
    }

    public double getTotalXp() {
        return totalXp;
    }
}
