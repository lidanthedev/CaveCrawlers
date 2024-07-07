package me.lidan.cavecrawlers.bosses;

import lombok.Getter;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Getter
public class BossDrops implements ConfigurationSerializable {
    private final List<BossDrop> drops;
    private final String entityName;
    private final ConfigMessage announce;

    public BossDrops(List<BossDrop> drops, String entityName, ConfigMessage announce) {
        this.drops = drops;
        this.entityName = entityName;
        this.announce = announce;
    }

    public void drop(Player player, int points) {
        drop(player, player.getLocation(), points);
    }

    public void drop(Player player, Location location, int points) {
        Set<String> gotDrops = new HashSet<>();
        for (BossDrop drop : drops) {
            String track = drop.getTrack();
            if (!gotDrops.contains(track) && points >= drop.getRequiredPoints() && drop.rollChance(player)) {
                gotDrops.add(track);
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
        ConfigMessage announce = ConfigMessage.getMessage(map.getOrDefault("announce", "").toString());
        return new BossDrops(deserializedDrops, entityName, announce);
    }
}
