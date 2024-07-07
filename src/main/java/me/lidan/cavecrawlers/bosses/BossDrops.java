package me.lidan.cavecrawlers.bosses;

import lombok.Getter;
import me.lidan.cavecrawlers.griffin.GriffinDrop;
import me.lidan.cavecrawlers.griffin.GriffinDrops;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class BossDrops implements ConfigurationSerializable {
    private final List<BossDrop> drops;
    private final String entityName;

    public BossDrops(List<BossDrop> drops, String entityName) {
        this.drops = drops;
        this.entityName = entityName;
    }

    public void drop(Player player, int points) {
        drop(player, player.getLocation(), points);
    }

    public void drop(Player player, Location location, int points) {
        Map<String, BossDrop> gotDrops = new HashMap<>();
        for (BossDrop drop : drops) {
            String track = drop.getTrack();
            if (!gotDrops.containsKey(track) && points >= drop.getRequiredPoints() && drop.rollChance(player)) {
                gotDrops.put(track, drop);
                drop.drop(player, location);
            }
        }
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        return Map.of("drops", drops);
    }

    public static BossDrops deserialize(Map<String, Object> map) {
        List<BossDrop> deserializedDrops = new ArrayList<>();
        List<Map<String, Object>> dropsList = (List<Map<String, Object>>) map.get("drops");
        for (Map<String, Object> dropMap : dropsList) {
            deserializedDrops.add(BossDrop.deserialize(dropMap));
        }
        String entityName = (String) map.get("entityName");
        return new BossDrops(deserializedDrops, entityName);
    }
}
