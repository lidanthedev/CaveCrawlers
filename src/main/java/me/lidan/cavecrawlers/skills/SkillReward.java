package me.lidan.cavecrawlers.skills;

import lombok.Data;
import org.bukkit.entity.Player;

@Data
public abstract class SkillReward {
    public abstract void applyReward(Player player);
}
