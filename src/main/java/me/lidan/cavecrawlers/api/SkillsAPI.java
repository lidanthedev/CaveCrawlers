package me.lidan.cavecrawlers.api;

import me.lidan.cavecrawlers.skills.SkillAction;
import me.lidan.cavecrawlers.skills.SkillInfo;
import org.bukkit.entity.Player;

/**
 * API for managing player skills in the CaveCrawlers plugin.
 * Implementations should provide methods for registering, retrieving, and updating skills.
 */
public interface SkillsAPI {
    void register(String key, SkillInfo value);

    SkillInfo getSkillInfo(String key);

    void tryGiveXp(SkillInfo skillType, SkillAction reason, String material, Player player);

    void tryGiveXp(SkillAction reason, String material, Player player);
}
