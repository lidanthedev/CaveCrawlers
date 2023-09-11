package me.lidan.cavecrawlers.items;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.utils.CustomConfig;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ItemsLoader {
    private static ItemsLoader instance;
    private ItemsManager itemsManager;
    private Map<String, File> itemIDFileMap;

    public ItemsLoader(ItemsManager itemsManager) {
        this.itemsManager = itemsManager;
        this.itemIDFileMap = new HashMap<>();
    }

    public void registerItemsFromFolder(File dir) {
        if (!dir.exists()){
            dir.mkdirs();
        }

        File[] files = dir.listFiles();

        // loop through files
        for(File file : files) {
            if (file.isDirectory()){
                registerItemsFromFolder(file);
            }
            else{
                registerItemsFromFile(file);
            }
        }
    }

    private void registerItemsFromFile(File file) {
        CustomConfig customConfig = new CustomConfig(file);
        Set<String> registered = registerItemsFromConfig(customConfig);
        for (String s : registered) {
            itemIDFileMap.put(s, file);
        }
    }

    private Set<String> registerItemsFromConfig(FileConfiguration configuration){
        Set<String> registeredItems = new HashSet<>();
        Set<String> keys = configuration.getKeys(false);
        for (String key : keys) {
            ItemInfo itemInfo = configuration.getObject(key, ItemInfo.class);
            if (itemInfo != null) {
                registeredItems.add(key);
                itemsManager.registerItem(key, itemInfo);
            }
            else {
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
