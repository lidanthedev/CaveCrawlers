package me.lidan.cavecrawlers.skills;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.lidan.cavecrawlers.stats.Stat;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
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
    public Component getRewardMessage() {
        return MiniMessageUtils.miniMessageString("<green>+<amount> <stat></green>", Map.of("amount", String.valueOf(stat.getValue()), "stat", stat.getType().getName()));
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("stat", "%s %s".formatted(stat.getType(), stat.getValue()));
        return map;
    }
}
