package me.lidan.cavecrawlers.drops;

import lombok.Getter;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class EntityDrops implements ConfigurationSerializable {
    private final String entityName;
    private final List<Drop> dropList;

    public EntityDrops(String entityName, List<Drop> dropList) {
        this.entityName = entityName;
        this.dropList = dropList;
    }

    public void roll(Player player){
        for (Drop drop : dropList) {
            drop.roll(player);
        }
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("entityName", entityName);
        map.put("drops", dropList.stream().map(Drop::serialize).toList());
        return map;
    }

    public static EntityDrops deserialize(Map<String, Object> map){
        String entityName = (String) map.get("entityName");

        List<Map<String, Object>> dropsList = (List<Map<String, Object>>) map.get("drops");

        List<Drop> drops = dropsList.stream()
                .map(Drop::deserialize)
                .toList();

        return new EntityDrops(entityName, drops);
    }
}
