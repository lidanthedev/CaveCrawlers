package me.lidan.cavecrawlers.drops;

import me.lidan.cavecrawlers.api.DropsAPI;
import net.md_5.bungee.api.ChatColor;

import java.util.HashMap;
import java.util.Map;

public class DropsManager implements DropsAPI {
    private static DropsManager instance;

    private final Map<String, EntityDrops> entityDropsMap = new HashMap<>();

    @Override
    public void register(String entityName, EntityDrops entityDrops){
        entityName = ChatColor.translateAlternateColorCodes('&', entityName);
        entityDropsMap.put(entityName, entityDrops);
    }

    @Override
    public EntityDrops getEntityDrops(String entityName){
        return entityDropsMap.get(entityName);
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
