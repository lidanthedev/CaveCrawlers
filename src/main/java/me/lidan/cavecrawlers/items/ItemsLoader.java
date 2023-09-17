package me.lidan.cavecrawlers.items;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.objects.ConfigLoader;

import java.io.File;

public class ItemsLoader extends ConfigLoader<ItemInfo> {
    private static ItemsLoader instance;
    private final ItemsManager itemsManager;

    public ItemsLoader(ItemsManager itemsManager) {
        super(ItemInfo.class, "items");
        this.itemsManager = itemsManager;
    }

    @Override
    public void register(String key, ItemInfo value) {
        itemsManager.registerItem(key, value);
    }

    public static void delete(){
        instance = null;
    }

    public static ItemsLoader getInstance() {
        if (instance == null){
            instance = new ItemsLoader(ItemsManager.getInstance());
        }
        return instance;
    }
}
