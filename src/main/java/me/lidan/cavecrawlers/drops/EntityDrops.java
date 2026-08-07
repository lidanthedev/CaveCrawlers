package me.lidan.cavecrawlers.drops;

import io.lumine.mythic.api.mobs.MythicMob;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.integration.mythic.MythicMobsHook;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
public class EntityDrops implements ConfigurationSerializable {
    private final String entityId;
    private final List<Drop> dropList;
    private final int xp;

    public EntityDrops(String entityId, List<Drop> dropList, int xp) {
        this.entityId = entityId;
        this.dropList = dropList;
        this.xp = xp;
    }

    public void roll(Player player){
        player.giveExp(xp);
        for (Drop drop : dropList) {
            drop.roll(player);
        }
    }

    public static EntityDrops deserialize(Map<String, Object> map){
        String entityId = (String) map.get("entityId");
        if (entityId == null && map.containsKey("entityName")) {
            String entityName = (String) map.get("entityName");
            MythicMob mob = MythicMobsHook.getInstance().getMobByName(entityName);
            entityId = mob != null ? mob.getInternalName() : null;
            if (entityId == null) {
                log.warn("Failed to migrate entity");
                throw new IllegalArgumentException("Failed to migrate entity");
            }
            log.info("Migrated entity: {} from {}", entityId, entityName);
        }

        int xp = (int) map.get("xp");

        List<Drop> drops = null;
        try {
            // Legacy support for serialized drops as maps
            List<Map<String, Object>> dropsList = (List<Map<String, Object>>) map.get("drops");

            drops = dropsList.stream()
                    .map(Drop::deserialize)
                    .toList();
        } catch (ClassCastException e) {
            drops = (List<Drop>) map.get("drops");
        }

        return new EntityDrops(entityId, drops, xp);
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("entityId", entityId);
        map.put("xp", xp);
        map.put("drops", dropList);
        return map;
    }
}
