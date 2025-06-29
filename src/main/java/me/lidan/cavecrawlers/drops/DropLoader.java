package me.lidan.cavecrawlers.drops;

import me.lidan.cavecrawlers.objects.ConfigLoader;

// Todo: make this not a singleton and allow multiple loaders for adddon support
public class DropLoader extends ConfigLoader<EntityDrops> {

    private static DropLoader instance;
    private final DropsManager dropsManager = DropsManager.getInstance();

    private DropLoader() {
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
