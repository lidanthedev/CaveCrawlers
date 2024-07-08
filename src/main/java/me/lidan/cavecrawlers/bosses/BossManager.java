package me.lidan.cavecrawlers.bosses;

import net.md_5.bungee.api.ChatColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class BossManager {
    private static final Logger log = LoggerFactory.getLogger(BossManager.class);
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
