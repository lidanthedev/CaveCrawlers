package me.lidan.cavecrawlers.bosses;

import me.lidan.cavecrawlers.objects.ConfigLoader;

public class BossLoader extends ConfigLoader<BossDrops> {
    private static BossLoader instance;

    public BossLoader() {
        super(BossDrops.class, "bosses");
    }

    @Override
    public void register(String key, BossDrops value) {
        BossManager.getInstance().registerEntityDrops(value.getEntityName(), value);
    }

    public static BossLoader getInstance() {
        if (instance == null){
            instance = new BossLoader();
        }
        return instance;
    }
}
