package me.lidan.cavecrawlers.griffin;

import me.lidan.cavecrawlers.objects.ConfigLoader;

public class GriffinLoader extends ConfigLoader<GriffinDrops> {
    private static GriffinLoader instance;
    private GriffinManager griffinManager;

    public GriffinLoader() {
        super(GriffinDrops.class, "griffin");
        griffinManager = GriffinManager.getInstance();
    }

    @Override
    public void register(String key, GriffinDrops value) {
        griffinManager.registerDrop(key, value);
    }

    public static GriffinLoader getInstance() {
        if (instance == null) {
            instance = new GriffinLoader();
        }
        return instance;
    }
}
