package me.lidan.cavecrawlers.drops;

import me.lidan.cavecrawlers.objects.ConfigLoader;

public class DropLoader extends ConfigLoader<EntityDrops> {

    private static DropLoader instance;
    private final DropsManager dropsManager = DropsManager.getInstance();

    public DropLoader() {
        super(EntityDrops.class, "drops");
    }

    @Override
    public void register(String key, EntityDrops value) {
        dropsManager.register(value.getEntityName(), value);
    }

    public static DropLoader getInstance() {
        if (instance == null){
            instance = new DropLoader();
        }
        return instance;
    }

    @Override
    public void clear() {
        super.clear();
        dropsManager.clear();
    }
}
