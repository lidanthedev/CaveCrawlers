package me.lidan.cavecrawlers.drops;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityDrops implements ConfigurationSerializable {
    private final String entityName;
    private final List<Drop> dropList;

    public EntityDrops(String entityName, List<Drop> dropList) {
        this.entityName = entityName;
        this.dropList = dropList;
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("entityName", entityName);
        map.put("drops", dropList);
        return map;
    }

    public static EntityDrops deserialize(Map<String, Object> map){
        String entityName = (String) map.getOrDefault("entityName", "");
        List<Drop> dropList = (List<Drop>) map.getOrDefault("drops", List.of());

        return new EntityDrops(entityName, dropList);
    }
}
