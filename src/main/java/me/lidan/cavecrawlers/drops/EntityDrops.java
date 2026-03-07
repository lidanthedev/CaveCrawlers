package me.lidan.cavecrawlers.drops;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class EntityDrops implements ConfigurationSerializable {
    private String entityName;
    private List<Drop> dropList;
    private int xp;

    public EntityDrops(String entityName, List<Drop> dropList, int xp) {
        this.entityName = entityName;
        this.dropList = dropList;
        this.xp = xp;
    }

    /**
     * Creates a deep copy of this EntityDrops instance.
     * The drop list is copied with each Drop cloned individually.
     *
     * @return a new EntityDrops with copied fields
     */
    public EntityDrops deepCopy() {
        List<Drop> copiedDrops = new ArrayList<>();
        for (Drop drop : dropList) {
            copiedDrops.add(drop.clone());
        }
        return new EntityDrops(entityName, copiedDrops, xp);
    }

    public void roll(Player player){
        player.giveExp(xp);
        for (Drop drop : dropList) {
            drop.roll(player);
        }
    }

    public static EntityDrops deserialize(Map<String, Object> map){
        String entityName = (String) map.get("entityName");
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

        return new EntityDrops(entityName, drops, xp);
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("entityName", entityName);
        map.put("xp", xp);
        map.put("drops", dropList);
        return map;
    }
}
