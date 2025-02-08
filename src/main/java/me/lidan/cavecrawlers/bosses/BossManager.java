package me.lidan.cavecrawlers.bosses;

import net.md_5.bungee.api.ChatColor;

import java.util.HashMap;
import java.util.Map;

public class BossManager {
    private static BossManager instance;
    private final Map<String, BossDrops> dropsMap = new HashMap<>();

    public void registerEntityDrops(String entityName, BossDrops entityDrops) {
        entityName = ChatColor.translateAlternateColorCodes('&', entityName);
        dropsMap.put(entityName, entityDrops);
    }

    public BossDrops getEntityDrops(String name) {
        return dropsMap.get(name);
    }

    public static BossManager getInstance() {
        if (instance == null) {
            instance = new BossManager();
        }
        return instance;
    }
}
