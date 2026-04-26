package me.lidan.cavecrawlers.storage;

import me.lidan.cavecrawlers.skills.SkillsManager;
import me.lidan.cavecrawlers.skills.Skills;
import me.lidan.cavecrawlers.stats.Stats;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlayerDataManager {
    private static PlayerDataManager instance;

    private final SkillsManager skillsManager;

    private PlayerDataManager() {
        this.skillsManager = SkillsManager.getInstance();
    }

    public void saveAll(){
        skillsManager.saveAllAsync();
    }

    public void saveAllAndWait() {
        skillsManager.saveAllAndWait();
    }

    public PlayerData loadPlayerData(UUID player){
        skillsManager.loadPlayerSync(player);
        return new PlayerData(skillsManager.getSkills(player));
    }

    public void savePlayerData(UUID player){
        skillsManager.savePlayerDataAsync(player);
    }

    public void savePlayerDataInMap(UUID player, PlayerData playerData) {
        if (playerData == null) {
            return;
        }
        Skills skills = playerData.getSkills();
        if (skills == null) {
            skills = new Skills();
        }
        skills.setUuid(player);
        skillsManager.setSkills(player, skills);
    }

    public PlayerData getPlayerData(UUID uuid){
        Skills skills = skillsManager.getSkills(uuid);
        skills.setUuid(uuid);
        return new PlayerData(skills);
    }

    public PlayerData getPlayerData(Player player){
        return getPlayerData(player.getUniqueId());
    }

    public static PlayerDataManager getInstance() {
        if (instance == null){
            instance = new PlayerDataManager();
        }
        return instance;
    }
    public Stats getStatsFromSkills(Player player) {
        Skills skills = getSkills(player);
        return skills.getStats();
    }

    public Skills getSkills(Player player) {
        return skillsManager.getSkills(player);
    }

    public void resetPlayerData(@NotNull UUID uniqueId) {
        Skills skills = new Skills();
        skills.setUuid(uniqueId);
        skillsManager.setSkills(uniqueId, skills);
        skillsManager.savePlayerDataAsync(uniqueId);
    }
}
