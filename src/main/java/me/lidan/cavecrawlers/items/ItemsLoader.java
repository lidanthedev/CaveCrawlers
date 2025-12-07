package me.lidan.cavecrawlers.items;

import lombok.Getter;
import me.lidan.cavecrawlers.objects.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ItemsLoader extends ConfigLoader<ItemInfo> {
    private static final Logger log = LoggerFactory.getLogger(ItemsLoader.class);
    private static ItemsLoader instance;
    private final ItemsManager itemsManager;
    @Getter
    private final Map<String, ItemInfo> notFullyLoadedItems = new HashMap<>();

    private ItemsLoader(ItemsManager itemsManager) {
        super(ItemInfo.class, "items");
        this.itemsManager = itemsManager;
    }

    @Override
    public void register(String key, ItemInfo value) {
        // Only warn once if the item is not fully loaded
        if (!value.isFullyLoaded() && !itemsManager.getKeys().contains(key)) {
            notFullyLoadedItems.putIfAbsent(key, value);
            log.warn("Item {} is not fully loaded", key);
        }
        itemsManager.registerItem(key, value);
    }

    @Override
    public void clear() {
        super.clear();
        itemsManager.clear();
    }

    public static ItemsLoader getInstance() {
        if (instance == null){
            instance = new ItemsLoader(ItemsManager.getInstance());
        }
        return instance;
    }
}
