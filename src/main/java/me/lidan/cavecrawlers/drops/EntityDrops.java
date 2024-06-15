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
    private final List<SimpleDrop> dropList;
    private final int xp;

    public EntityDrops(String entityName, List<SimpleDrop> dropList, int xp) {
        this.entityName = entityName;
        this.dropList = dropList;
        this.xp = xp;
    }

    public void roll(Player player){
        player.giveExp(xp);
        for (SimpleDrop drop : dropList) {
            drop.roll(player);
        }
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("entityName", entityName);
        map.put("xp", xp);
        map.put("drops", dropList.stream().map(SimpleDrop::serialize).toList());
        return map;
    }

    public static EntityDrops deserialize(Map<String, Object> map){
        String entityName = (String) map.get("entityName");
        int xp = (int) map.get("xp");

        List<Map<String, Object>> dropsList = (List<Map<String, Object>>) map.get("drops");

        List<SimpleDrop> drops = dropsList.stream()
                .map(SimpleDrop::deserialize)
                .toList();

        return new EntityDrops(entityName, drops, xp);
    }
}
