package me.lidan.cavecrawlers.bosses;

import me.lidan.cavecrawlers.api.BossAPI;
import net.md_5.bungee.api.ChatColor;

import java.util.HashMap;
import java.util.Map;

public class BossManager implements BossAPI {
    private static BossManager instance;
    private final Map<String, BossDrops> dropsMap = new HashMap<>();

    @Override
    public void registerEntityDrops(String entityName, BossDrops entityDrops) {
        entityName = ChatColor.translateAlternateColorCodes('&', entityName);
        dropsMap.put(entityName, entityDrops);
    }

    @Override
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
