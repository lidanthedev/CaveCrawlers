package me.lidan.cavecrawlers.entities;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.api.EntityAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityManager implements EntityAPI {
    private static final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private static EntityManager instance;
    private final Map<UUID, EntityData> entityDataMap = new HashMap<>();

    private boolean isLootShareEnabledByDefault() {
        return plugin.getConfig().getBoolean("loot-share.enable-by-default", true);
    }

    private int getLootShareDamageThresholdPercent() {
        return plugin.getConfig().getInt("loot-share.damage-threshold", 10);
    }

    public @Nullable EntityData getEntityData(UUID entityUuid) {
        return entityDataMap.get(entityUuid);
    }

    @Override
    public void setEntityData(UUID entityUuid, EntityData entityData) {
        entityDataMap.put(entityUuid, entityData);
    }

    @Override
    public void addDamage(UUID playerUuid, Entity entity, double damage) {
        if (entity instanceof LivingEntity livingEntity) {
            EntityData entityData = entityDataMap.computeIfAbsent(entity.getUniqueId(), uuid -> {
                if (isLootShareEnabledByDefault()) {
                    return new LootShareEntityData(livingEntity, getLootShareDamageThresholdPercent(), playerUuid);
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
        }
    }

    public void onEntityRemove(Entity entity) {
        entityDataMap.remove(entity.getUniqueId());
    }

    public void sweep() {
        entityDataMap.entrySet().removeIf(entry -> {
            EntityData data = entry.getValue();
            return data.entity == null || data.entity.isDead() || !data.entity.isValid();
        });
    }

    public void clear() {
        entityDataMap.clear();
    }

    public void startSweepTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::sweep, 20L * 60 * 5, 20L * 60 * 5);
    }

    public static EntityManager getInstance() {
        if (instance == null) {
            instance = new EntityManager();
        }
        return instance;
    }
}
