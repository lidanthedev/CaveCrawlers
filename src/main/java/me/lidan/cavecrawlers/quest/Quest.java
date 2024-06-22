package me.lidan.cavecrawlers.quest;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class Quest {
    private String Id;
    Map<UUID, Boolean> finishQuestMap = new HashMap<>();
    public abstract void startQuest(UUID uuid);
    public void finishQuest(UUID uuid){
        finishQuestMap.put(uuid, true);
    }
    public int progress(UUID uuid){
        finishQuest(uuid);
        return 0;
    }
    public int progress(Player player){
        return progress(player.getUniqueId());
    }
}
