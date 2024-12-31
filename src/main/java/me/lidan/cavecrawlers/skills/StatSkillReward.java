package me.lidan.cavecrawlers.skills;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.lidan.cavecrawlers.stats.Stat;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class StatSkillReward extends SkillReward {
    private final Stat stat;

    public StatSkillReward(Stat stats) {
        this.stat = stats;
    }

    @Override
    public void applyReward(Player player) {

    }
}
