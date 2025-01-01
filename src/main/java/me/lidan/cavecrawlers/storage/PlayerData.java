package me.lidan.cavecrawlers.storage;

import lombok.Data;
import me.lidan.cavecrawlers.skills.Skills;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class PlayerData implements ConfigurationSerializable {
    private Skills skills;

    public PlayerData() {
        this(new Skills());
    }

    public PlayerData(Skills skills) {
        this.skills = skills;
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
        skills.setUuid(uuid);
//        CustomConfig config = new CustomConfig("players/" + uuid + ".yml");
//        if (config.contains("skills")) {
//            skills = (Skills) config.get("skills");
//        }
    }

    public void savePlayer(UUID uuid) {
//        CustomConfig config = new CustomConfig("players/" + uuid + ".yml");
//        config.set("skills", skills);
//        config.save();
    }
}
