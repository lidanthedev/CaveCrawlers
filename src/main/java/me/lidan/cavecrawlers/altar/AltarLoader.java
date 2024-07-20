package me.lidan.cavecrawlers.altar;

import me.lidan.cavecrawlers.objects.ConfigLoader;

public class AltarLoader extends ConfigLoader<Altar> {
    private static AltarLoader instance;
    private final AltarManager altarManager = AltarManager.getInstance();

    private AltarLoader() {
        super(Altar.class, "altars");
    }

    @Override
    public void register(String key, Altar value) {
        altarManager.registerAltar(key, value);
    }

    public static AltarLoader getInstance() {
        if (instance == null) {
            instance = new AltarLoader();
        }
        return instance;
    }
}
