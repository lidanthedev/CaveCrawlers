package me.lidan.cavecrawlers.bosses;

import lombok.Getter;
import me.lidan.cavecrawlers.drops.Drop;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Getter
public class BossDrop extends Drop implements ConfigurationSerializable {
    private int requiredPoints;
    private String track;

    public BossDrop(String type, double chance, String value, int requiredPoints, String track) {
        super(type, chance, value);
        this.requiredPoints = requiredPoints;
        this.track = track;
    }

    public BossDrop(String type, double chance, String value, int requiredPoints) {
        this(type, chance, value, requiredPoints, null);
    }

    public BossDrop(String type, double chance, String value) {
        this(type, chance, value, 0, null);
    }

    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("requiredPoints", requiredPoints);
        if (track != null) {
            map.put("track", track);
        }
        return map;
    }

    public static BossDrop deserialize(Map<String, Object> map) {
        String type = (String) map.get("type");
        double chance = (double) map.get("chance");
        String value = (String) map.get("value");
        int requiredPoints = (int) map.get("requiredPoints");
        String track = (String) map.get("track");
        return new BossDrop(type, chance, value, requiredPoints, track);
    }
}
