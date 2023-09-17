package me.lidan.cavecrawlers.items;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.utils.CustomConfig;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ItemsLoader extends ConfigLoader {
    private static ItemsLoader instance;
    private final ItemsManager itemsManager;
    public final File ITEMS_DIR_FILE = new File(CaveCrawlers.getInstance().getDataFolder(), "items");

    public ItemsLoader(ItemsManager itemsManager) {
        this.itemsManager = itemsManager;
    }

    @Override
    public void load() {
        registerItemsFromFolder(ITEMS_DIR_FILE);
    }

    @Override
    public Set<String> registerItemsFromConfig(FileConfiguration configuration) {
        Set<String> registeredItems = new HashSet<>();
        Set<String> keys = configuration.getKeys(false);
        for (String key : keys) {
            ItemInfo itemInfo = configuration.getObject(key, ItemInfo.class);
            if (itemInfo != null) {
                registeredItems.add(key);
                itemsManager.registerItem(key, itemInfo);
            } else {
                CaveCrawlers.getInstance().getLogger().warning("Failed to Load Item: " + key);
            }
        }
        return registeredItems;
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
