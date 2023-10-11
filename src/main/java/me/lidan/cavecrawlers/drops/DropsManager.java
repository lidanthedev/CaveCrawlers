package me.lidan.cavecrawlers.drops;

import java.util.HashMap;
import java.util.Map;

public class DropsManager {
    private static DropsManager instance;

    private final Map<String, EntityDrops> entityDropsMap = new HashMap<>();

    public void register(String entityName, EntityDrops entityDrops){
        entityDropsMap.put(entityName, entityDrops);
    }

    public EntityDrops getEntityDrops(String entityName){
        return entityDropsMap.get(entityName);
    }

    public static DropsManager getInstance() {
        if (instance == null){
            instance = new DropsManager();
        }
        return instance;
    }
}
