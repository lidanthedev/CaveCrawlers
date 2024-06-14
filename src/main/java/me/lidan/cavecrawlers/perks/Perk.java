package me.lidan.cavecrawlers.perks;

import lombok.Data;
import me.lidan.cavecrawlers.stats.Stats;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Data
public class Perk implements ConfigurationSerializable {
    private String name;
    private String track;
    private String permission;
    private int priority;
    private Stats stats;

    public Perk(String name, String track, String permission, int priority, Stats stats) {
        this.name = name;
        this.track = track;
        this.permission = permission;
        this.priority = priority;
        this.stats = stats;
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        return Map.of(
                "name", name,
                "track", track,
                "permission", permission,
                "stats", stats
        );
    }

    public static Perk deserialize(Map<String, Object> map) {
        Map<String, Object> statsMap = (Map<String, Object>) map.get("stats");
        Stats stats = Stats.deserialize(statsMap);
        return new Perk(
                (String) map.get("name"),
                (String) map.get("track"),
                (String) map.get("permission"),
                (int) map.get("priority"),
                stats
        );
    }
}
