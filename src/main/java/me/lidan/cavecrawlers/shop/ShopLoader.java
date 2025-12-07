package me.lidan.cavecrawlers.shop;

import me.lidan.cavecrawlers.objects.ConfigLoader;

public class ShopLoader extends ConfigLoader<ShopMenu> {
    private static ShopLoader instance;
    private final ShopManager shopManager;

    private ShopLoader() {
        super(ShopMenu.class, "shops");
        shopManager = ShopManager.getInstance();
    }

    @Override
    public void register(String key, ShopMenu value) {
        shopManager.registerMenu(key, value);
    }

    @Override
    public void clear() {
        super.clear();
        shopManager.clear();
    }

    public static ShopLoader getInstance(){
        if (instance == null){
            instance = new ShopLoader();
        }
        return instance;
    }
}
