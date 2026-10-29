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
                "priority", priority,
                "stats", stats.serialize()
        );
    }

    public static Perk deserialize(Map<String, Object> map) {
        Object rawStats = map.get("stats");
        Stats stats = rawStats instanceof Stats value
                ? value
                : Stats.deserialize((Map<String, Object>) rawStats);
        Object rawPriority = map.getOrDefault("priority", 0);
        return new Perk(
                (String) map.get("name"),
                (String) map.get("track"),
                (String) map.get("permission"),
                ((Number) rawPriority).intValue(),
                stats
        );
    }
}
