package me.lidan.cavecrawlers.drops;

import lombok.Getter;
import me.lidan.cavecrawlers.api.DropsAPI;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DropsManager implements DropsAPI {
    private static DropsManager instance;

    @Getter
    private final Map<String, EntityDrops> entityDropsMap = new HashMap<>();

    @Override
    public void register(String entityId, EntityDrops entityDrops) {
        entityDropsMap.put(entityId, entityDrops);
    }

    @Override
    public EntityDrops getEntityDrops(String entityId) {
        return entityDropsMap.get(entityId);
    }

    public void rollDropsForPlayer(Player player, List<Drop> drops) {
        for (Drop drop : drops) {
            drop.roll(player);
        }
    }

    public void clear(){
        entityDropsMap.clear();
    }

    public static DropsManager getInstance() {
        if (instance == null){
            instance = new DropsManager();
        }
        return instance;
    }
}
