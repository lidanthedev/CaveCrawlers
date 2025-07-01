package me.lidan.cavecrawlers.api;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * The main API interface for the CaveCrawlers plugin.
 * Provides access to all sub-APIs for abilities, action bars, bosses, damage, drops, entities, items, mining, prompts, skills, and stats.
 */
public interface CaveCrawlersAPI {
    /**
     * @return The AbilityAPI for managing abilities.
     */
    AbilityAPI getAbilityAPI();

    /**
     * @return The ActionBarAPI for handling action bar messages.
     */
    ActionBarAPI getActionBarAPI();

    /**
     * @return The BossAPI for managing bosses.
     */
    BossAPI getBossAPI();

    /**
     * @return The DamageAPI for handling custom damage logic.
     */
    DamageAPI getDamageAPI();

    /**
     * @return The DropsAPI for managing item drops.
     */
    DropsAPI getDropsAPI();

    /**
     * @return The EntityAPI for entity-related operations.
     */
    EntityAPI getEntityAPI();

    /**
     * @return The ItemsAPI for managing custom items.
     */
    ItemsAPI getItemsAPI();

    /**
     * @return The MiningAPI for mining-related features.
     */
    MiningAPI getMiningAPI();

    /**
     * @return The PromptAPI for handling prompts and messages.
     */
    PromptAPI getPromptAPI();

    /**
     * @return The SkillsAPI for managing player skills.
     */
    SkillsAPI getSkillsAPI();

    /**
     * @return The StatsAPI for managing player stats.
     */
    StatsAPI getStatsAPI();

    /**
     * Registers all configs from the plugin's folder.
     *
     * @param plugin the JavaPlugin instance to register configs from
     */
    void registerFromConfigs(JavaPlugin plugin);
}
