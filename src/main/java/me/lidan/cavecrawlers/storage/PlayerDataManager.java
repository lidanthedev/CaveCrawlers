package me.lidan.cavecrawlers.storage;

import me.lidan.cavecrawlers.skills.Skills;
import me.lidan.cavecrawlers.stats.Stats;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlayerDataManager {
    private static PlayerDataManager instance;

    private PlayerDataManager() {
    }

    public static PlayerDataManager getInstance() {
        if (instance == null) {
            instance = new PlayerDataManager();
        }
        return instance;
    }

    public void saveAll() {
        PlayerSkillsManager.getInstance().saveAll();
    }

    public PlayerData loadPlayerData(UUID uuid) {
        Skills skills = PlayerSkillsManager.getInstance().loadPlayerSync(uuid);
        return new PlayerData(skills);
    }

    public void savePlayerData(UUID uuid) {
        PlayerSkillsManager.getInstance().savePlayerAsync(uuid);
    }

    public void savePlayerDataInMap(UUID uuid, PlayerData playerData) {
        PlayerSkillsManager.getInstance().putSkills(uuid, playerData.getSkills());
    }

    public PlayerData getPlayerData(UUID uuid) {
        return new PlayerData(PlayerSkillsManager.getInstance().getSkills(uuid));
    }

    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    public Stats getStatsFromSkills(Player player) {
        return getSkills(player).getStats();
    }

    public Skills getSkills(Player player) {
        return PlayerSkillsManager.getInstance().getSkills(player);
    }

    public void resetPlayerData(@NotNull UUID uuid) {
        PlayerSkillsManager.getInstance().resetPlayerData(uuid);
    }
}
