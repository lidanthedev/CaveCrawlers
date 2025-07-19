package me.lidan.cavecrawlers.api;

import me.lidan.cavecrawlers.skills.SkillAction;
import me.lidan.cavecrawlers.skills.SkillInfo;
import org.bukkit.entity.Player;

/**
 * API for managing player skills in the CaveCrawlers plugin.
 * Implementations should provide methods for registering, retrieving, and updating skills.
 */
public interface SkillsAPI {
    /**
     * Registers a skill with a given key and SkillInfo.
     *
     * @param key   the unique identifier for the skill
     * @param value the SkillInfo to register
     */
    void register(String key, SkillInfo value);

    /**
     * Retrieves the SkillInfo for a given key.
     *
     * @param key the unique identifier for the skill
     * @return the SkillInfo associated with the key, or null if not found
     */
    SkillInfo getSkillInfo(String key);

    /**
     * Attempts to give XP to a player for a specific skill, action, and material.
     *
     * @param skillType the SkillInfo representing the skill
     * @param reason the SkillAction reason for XP gain
     * @param material the material involved in the action
     * @param player the player to give XP to
     */
    void tryGiveXp(SkillInfo skillType, SkillAction reason, String material, Player player);

    /**
     * Attempts to give XP to a player for a specific action and material (skill inferred).
     *
     * @param reason the SkillAction reason for XP gain
     * @param material the material involved in the action
     * @param player the player to give XP to
     */
    void tryGiveXp(SkillAction reason, String material, Player player);
}
