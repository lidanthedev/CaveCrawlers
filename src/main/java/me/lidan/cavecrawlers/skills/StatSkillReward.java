package me.lidan.cavecrawlers.skills;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.lidan.cavecrawlers.stats.Stat;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

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

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("stat", "%s %s".formatted(stat.getType(), stat.getValue()));
        return map;
    }
}
