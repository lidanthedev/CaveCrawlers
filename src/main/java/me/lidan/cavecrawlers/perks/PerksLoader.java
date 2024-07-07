package me.lidan.cavecrawlers.perks;

import me.lidan.cavecrawlers.objects.ConfigLoader;

public class PerksLoader extends ConfigLoader<Perk> {
    private static PerksLoader instance;
    private PerksManager manager = PerksManager.getInstance();

    private PerksLoader() {
        super(Perk.class, "perks");
        manager = PerksManager.getInstance();
    }

    @Override
    public void register(String key, Perk value) {
        manager.register(key, value);
    }

    public static PerksLoader getInstance() {
        if (instance == null) {
            instance = new PerksLoader();
        }
        return instance;
    }
}
