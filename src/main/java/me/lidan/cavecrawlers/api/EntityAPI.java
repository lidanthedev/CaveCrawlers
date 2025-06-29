package me.lidan.cavecrawlers.api;

import me.lidan.cavecrawlers.entities.EntityData;
import org.bukkit.entity.Entity;

import java.util.UUID;

/**
 * API for entity-related operations in the CaveCrawlers plugin.
 * Implementations should provide methods for interacting with and managing entities.
 */
public interface EntityAPI {
    /**
     * Sets custom data for an entity by its UUID.
     *
     * @param entityUuid the UUID of the entity
     * @param entityData the custom data to associate with the entity
     */
    void setEntityData(UUID entityUuid, EntityData entityData);

    /**
     * Adds damage to an entity for a specific player.
     *
     * @param playerUuid the UUID of the player
     * @param entity     the entity to add damage to
     * @param damage     the amount of damage to add
     */
    void addDamage(UUID playerUuid, Entity entity, double damage);

    /**
     * Gets the total damage a player has dealt to an entity.
     *
     * @param playerUuid the UUID of the player
     * @param entity the entity to check
     * @return the total damage dealt by the player to the entity
     */
    double getDamage(UUID playerUuid, Entity entity);
}
