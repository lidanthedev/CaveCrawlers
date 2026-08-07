package me.lidan.cavecrawlers.bosses;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.drops.DropLoader;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Getter
@Slf4j
public class BossDrops implements ConfigurationSerializable {
    private final List<BossDrop> drops;
    private final String mobId;
    private final ConfigMessage announce;
    private final List<Integer> bonusPoints;

    public BossDrops(List<BossDrop> drops, String mobId, ConfigMessage announce, List<Integer> bonusPoints) {
        this.drops = drops;
        this.mobId = mobId;
        this.announce = announce;
        this.bonusPoints = bonusPoints;
    }

    public void drop(Player player, int points) {
        drop(player, player.getLocation(), points);
    }

    public static BossDrops deserialize(Map<String, Object> map) {
        List<BossDrop> dropsList = (List<BossDrop>) map.get("drops");

        String mobId = DropLoader.getOrMigrateMobId(map);
        ConfigMessage announce = ConfigMessage.getMessage((String) map.getOrDefault("announce", ""));
        List<Integer> bonusPoints = (List<Integer>) map.getOrDefault("bonusPoints", List.of(300, 250, 200, 150, 100));
        return new BossDrops(dropsList, mobId, announce, bonusPoints);
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put("drops", drops);
        serialized.put("mobId", mobId);
        serialized.put("announce", ConfigMessage.getIdOfMessage(announce));
        serialized.put("bonusPoints", bonusPoints);
        return serialized;
    }

    public void drop(Player player, Location location, int points) {
        Set<String> gotDrops = new HashSet<>();
        for (BossDrop drop : drops) {
            String track = drop.getTrack();
            if (!gotDrops.contains(track) && points >= drop.getRequiredPoints() && drop.rollChance(player)) {
                if (track != null) {
                    gotDrops.add(track);
                }
                drop.drop(player, location);
            }
        }
    }
}
