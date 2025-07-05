package me.lidan.cavecrawlers.items;

import me.lidan.cavecrawlers.objects.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemsLoader extends ConfigLoader<ItemInfo> {
    private static final Logger log = LoggerFactory.getLogger(ItemsLoader.class);
    private static ItemsLoader instance;
    private final ItemsManager itemsManager;

    private ItemsLoader(ItemsManager itemsManager) {
        super(ItemInfo.class, "items");
        this.itemsManager = itemsManager;
    }

    @Override
    public void register(String key, ItemInfo value) {
        if (!value.isFullyLoaded()) {
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
