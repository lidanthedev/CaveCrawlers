package me.lidan.cavecrawlers.storage;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.skills.Skills;
import me.lidan.cavecrawlers.stats.Stats;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {
    private static final String DIR_NAME = "players";
    private static PlayerDataManager instance;

    private Map<UUID, PlayerData> uuidPlayerDataMap;

    public PlayerDataManager(Map<UUID, PlayerData> uuidPlayerDataMap) {
        this.uuidPlayerDataMap = uuidPlayerDataMap;
    }

    public PlayerDataManager() {
        this.uuidPlayerDataMap = new HashMap<>();
        File dir = new File(CaveCrawlers.getInstance().getDataFolder(), DIR_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public PlayerData loadPlayerData(UUID player){
        PlayerData playerData = new PlayerData();
        playerData.loadPlayer(player);
        return playerData;
    }

    public void savePlayerData(UUID player){
        PlayerData playerData = new PlayerData();
        playerData.savePlayer(player);
        uuidPlayerDataMap.put(player, playerData);
    }

    public PlayerData getPlayerData(UUID uuid){
        if (!uuidPlayerDataMap.containsKey(uuid)){
            PlayerData playerData = loadPlayerData(uuid);
            uuidPlayerDataMap.put(uuid, playerData);
        }
        return uuidPlayerDataMap.get(uuid);
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
        return getPlayerData(player).getSkills();
    }
}
