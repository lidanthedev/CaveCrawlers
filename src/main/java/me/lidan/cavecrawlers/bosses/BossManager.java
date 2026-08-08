package me.lidan.cavecrawlers.bosses;

import lombok.Getter;
import me.lidan.cavecrawlers.api.BossAPI;

import java.util.HashMap;
import java.util.Map;

public class BossManager implements BossAPI {
    private static BossManager instance;
    @Getter
    private final Map<String, BossDrops> dropsMap = new HashMap<>();

    @Override
    public void registerEntityDrops(String mobId, BossDrops entityDrops) {
        dropsMap.put(mobId, entityDrops);
    }

    @Override
    public BossDrops getEntityDrops(String mobId) {
        return dropsMap.get(mobId);
    }

    public void clear() {
        dropsMap.clear();
    }

    public static BossManager getInstance() {
        if (instance == null) {
            instance = new BossManager();
        }
        return instance;
    }
}
