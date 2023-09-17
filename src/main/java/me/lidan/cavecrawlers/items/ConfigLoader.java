package me.lidan.cavecrawlers.items;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.utils.CustomConfig;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class ConfigLoader {
    private Map<String, File> itemIDFileMap;

    public ConfigLoader() {
        this.itemIDFileMap = new HashMap<>();
    }

    public abstract void load();

    public void registerItemsFromFolder(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File[] files = dir.listFiles();

        // loop through files
        for (File file : files) {
            if (file.isDirectory()) {
                registerItemsFromFolder(file);
            } else {
                registerItemsFromFile(file);
            }
        }
    }

    public void registerItemsFromFile(File file) {
        CustomConfig customConfig = new CustomConfig(file);
        Set<String> registered = registerItemsFromConfig(customConfig);
        for (String s : registered) {
            itemIDFileMap.put(s, file);
        }
    }

    public abstract Set<String> registerItemsFromConfig(FileConfiguration configuration);
}
