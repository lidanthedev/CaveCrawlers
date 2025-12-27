package me.lidan.cavecrawlers.skills;

import lombok.Data;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import java.util.Map;

@Data
public abstract class SkillReward implements ConfigurationSerializable {
    public abstract void applyReward(Player player);

    public abstract Component getRewardMessage();

    public static SkillReward valueOf(String type) {
        // format <type> <args>
        // for stat: STAT <stat> <value>
        // for item: ITEM <item>
        // for coins: COINS <amount>
        // for command: COMMAND <command>
        String[] split = type.split(" ");
        return switch (split[0]) {
            case "STAT" -> StatSkillReward.deserialize(Map.of("stat", split[1] + " " + split[2]));
            case "ITEM" -> ItemSkillReward.deserialize(Map.of("item", split[1]));
            case "COINS" -> new CoinSkillReward(Integer.parseInt(split[1]));
            case "COMMAND" -> new CommandSkillReward(type.substring(8)); // remove "COMMAND "
            default -> null;
        };
    }
}
