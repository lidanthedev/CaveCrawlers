package me.lidan.cavecrawlers.skills;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.DropType;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class CommandSkillReward extends SkillReward {
    public static final int CHANCE = 100;
    private final String command;

    public CommandSkillReward(String command) {
        this.command = command;
    }

    public static CommandSkillReward deserialize(Map<String, Object> map) {
        return new CommandSkillReward((String) map.get("value"));
    }

    @Override
    public void applyReward(Player player) {
        Drop drop = new Drop(DropType.COMMAND, CHANCE, command, null);
        drop.drop(player);
    }

    @Override
    public Component getRewardMessage() {
        return MiniMessageUtils.miniMessageString(""); // hidden command reward
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", command);
        return map;
    }
}
