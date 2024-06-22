package me.lidan.cavecrawlers.quest;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class ProgressQuest extends Quest {
    Map<UUID, Integer> progressMap;
    int maxProgress;
    public ProgressQuest(int maxProgress) {
        this.progressMap = new HashMap<>();
        this.maxProgress = maxProgress;
    }

    @Override
    public int progress(UUID uuid) {
        int progress = progressMap.getOrDefault(uuid,0) + 1;
        progressMap.put(uuid,progress);
        if(progress >= maxProgress){
            finishQuest(uuid);
        }
        return maxProgress - progress;
    }
}
