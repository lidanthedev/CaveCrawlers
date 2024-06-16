package me.lidan.cavecrawlers.entities;

import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityManager {
    private static final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private static final Logger log = LoggerFactory.getLogger(EntityManager.class);
    private static final boolean LOOT_SHARE_BY_DEFAULT = plugin.getConfig().getBoolean("loot-share-by-default", true);
    private static EntityManager instance;
    private final Map<UUID, EntityData> entityDataMap = new HashMap<>();

    public void setEntityData(UUID entityUuid, EntityData entityData) {
        entityDataMap.put(entityUuid, entityData);
    }

    public void addDamage(UUID playerUuid, Entity entity, double damage) {
        if (entity instanceof LivingEntity livingEntity) {
            EntityData entityData = entityDataMap.computeIfAbsent(entity.getUniqueId(), uuid -> {
                if (LOOT_SHARE_BY_DEFAULT){
                    return new LootShareEntityData(livingEntity, 10, playerUuid);
                } else {
                    return new EntityData(livingEntity);
                }
            });
            entityData.addDamage(playerUuid, damage);
            entityDataMap.put(entity.getUniqueId(), entityData);
        }
    }

    public double getDamage(UUID playerUuid, Entity entity) {
        EntityData entityData = entityDataMap.get(entity.getUniqueId());
        if (entityData == null) {
            return 0.0;
        }
        return entityData.getDamage(playerUuid);
    }

    public void onDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        EntityData entityData = entityDataMap.get(entity.getUniqueId());
        if (entityData != null) {
            entityData.onDeath(event);
            entityDataMap.remove(entity.getUniqueId());
        }
    }

    public static EntityManager getInstance() {
        if (instance == null) {
            instance = new EntityManager();
        }
        return instance;
    }
}
