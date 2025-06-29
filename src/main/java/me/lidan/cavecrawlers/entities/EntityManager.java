package me.lidan.cavecrawlers.entities;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.api.EntityAPI;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityManager implements EntityAPI {
    private static final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private static final boolean LOOT_SHARE_BY_DEFAULT = plugin.getConfig().getBoolean("loot-share-by-default", true);
    private static EntityManager instance;
    private final Map<UUID, EntityData> entityDataMap = new HashMap<>();

    @Override
    public void setEntityData(UUID entityUuid, EntityData entityData) {
        entityDataMap.put(entityUuid, entityData);
    }

    @Override
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

    @Override
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
