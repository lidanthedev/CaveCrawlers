package me.lidan.cavecrawlers.Dragons;

import me.lidan.cavecrawlers.objects.ConfigLoader;

public class DragonLoader extends ConfigLoader<DragonDrops> {
    private static DragonLoader instance;
    private DragonManager dragonManager;

    public DragonLoader() {
        super(DragonDrops.class, "dragons");
        dragonManager = DragonManager.getInstance();
    }

    @Override
    public void register(String key, DragonDrops value) {
        dragonManager.registerDrop(key, value);
    }

    public static DragonLoader getInstance() {
        if (instance == null) {
            instance = new DragonLoader();
        }
        return instance;
    }
}
