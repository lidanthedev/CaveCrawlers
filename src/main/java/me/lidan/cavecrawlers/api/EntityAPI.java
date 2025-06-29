package me.lidan.cavecrawlers.api;

import me.lidan.cavecrawlers.entities.EntityData;
import org.bukkit.entity.Entity;

import java.util.UUID;

/**
 * API for entity-related operations in the CaveCrawlers plugin.
 * Implementations should provide methods for interacting with and managing entities.
 */
public interface EntityAPI {
    void setEntityData(UUID entityUuid, EntityData entityData);

    void addDamage(UUID playerUuid, Entity entity, double damage);

    double getDamage(UUID playerUuid, Entity entity);
}
