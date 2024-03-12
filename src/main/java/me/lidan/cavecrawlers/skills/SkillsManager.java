package me.lidan.cavecrawlers.skills;

import me.lidan.cavecrawlers.stats.Stats;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillsManager {
    private static SkillsManager instance;
    private Map<UUID, Skills> uuidSkillsMap;

    public SkillsManager(Map<UUID, Skills> uuidSkillsMap) {
        this.uuidSkillsMap = uuidSkillsMap;
    }

    public SkillsManager() {
        this.uuidSkillsMap = new HashMap<>();
    }

    public Skills getSkills(UUID uuid){
        if (!uuidSkillsMap.containsKey(uuid)){
            uuidSkillsMap.put(uuid, new Skills());
        }
        return uuidSkillsMap.get(uuid);
    }

    public Skills getSkills(Player player){
        return getSkills(player.getUniqueId());
    }

    public Stats getStats(Player player){
        return getSkills(player).getStats();
    }

    public static SkillsManager getInstance() {
        if (instance == null){
            instance = new SkillsManager();
        }
        return instance;
    }
}
