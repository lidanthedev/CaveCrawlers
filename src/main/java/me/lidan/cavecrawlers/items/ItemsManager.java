package me.lidan.cavecrawlers.items;

import java.util.HashMap;
import java.util.Map;

public class ItemsManager {
    private static ItemsManager instance;
    private Map<String, ItemInfo> itemsMap;

    public ItemsManager() {
        itemsMap = new HashMap<>();
    }



    public static ItemsManager getInstance() {
        if (instance == null){
            instance = new ItemsManager();
        }
        return instance;
    }
}
