package me.lidan.cavecrawlers.api;

import me.lidan.cavecrawlers.items.abilities.ItemAbility;
import org.jetbrains.annotations.Nullable;

/**
 * API for managing abilities in the CaveCrawlers plugin.
 * Implementations should provide methods for registering, retrieving, and handling abilities.
 */
public interface AbilityAPI {
    /**
     * Registers an ability with a unique ID.
     *
     * @param ID      the unique identifier for the ability
     * @param ability the ability instance to register
     */
    void registerAbility(String ID, ItemAbility ability);

    /**
     * Retrieves an ability by its unique ID.
     *
     * @param ID the unique identifier for the ability
     * @return the ItemAbility associated with the ID, or null if not found
     */
    @Nullable
    ItemAbility getAbilityByID(String ID);

    /**
     * Gets the ID for a given ability instance.
     *
     * @param ability the ItemAbility instance
     * @return the unique identifier for the ability, or null if not found
     */
    @Nullable
    String getIDbyAbility(ItemAbility ability);
}
