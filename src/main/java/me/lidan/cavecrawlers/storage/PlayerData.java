package me.lidan.cavecrawlers.storage;

import lombok.Data;
import me.lidan.cavecrawlers.skills.Skills;
import me.lidan.cavecrawlers.utils.CustomConfig;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class PlayerData implements ConfigurationSerializable {
    private Skills skills;
    private boolean loaded;

    public PlayerData() {
        this(new Skills());
    }

    public PlayerData(Skills skills) {
        this.skills = skills;
    }

    public Skills getSkills() {
        return skills;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("skills", skills);
        return map;
    }

    public static PlayerData deserialize(Map<String, Object> map) {
        return new PlayerData((Skills) map.get("skills"));
    }

    public void loadPlayer(UUID uuid) {
        CustomConfig config = new CustomConfig(getConfigFor(uuid));
        if (config.contains("skills")) {
            skills = (Skills) config.get("skills");
        }
        if (skills == null) {
            skills = new Skills();
        }
        skills.setUuid(uuid);
        loaded = true;
    }

    public void savePlayer(UUID uuid) {
        if (!loaded) {
            return;
        }
        CustomConfig config = new CustomConfig(getConfigFor(uuid));
        config.set("skills", skills);
        config.save();
    }

    private static @NotNull String getConfigFor(UUID uuid) {
        return "players/" + uuid + ".yml";
    }
}
