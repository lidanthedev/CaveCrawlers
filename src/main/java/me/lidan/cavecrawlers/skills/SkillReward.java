package me.lidan.cavecrawlers.skills;

import lombok.Data;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

@Data
public abstract class SkillReward implements ConfigurationSerializable {
    public abstract void applyReward(Player player);
}
