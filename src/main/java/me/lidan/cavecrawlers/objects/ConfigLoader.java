package me.lidan.cavecrawlers.objects;

import lombok.Getter;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.utils.CustomConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class ConfigLoader<T extends ConfigurationSerializable> {
    private final Class<T> type;
    @Getter
    private final Map<String, File> itemIDFileMap;
    @Getter
    private final File fileDir;
    private final JavaPlugin plugin = JavaPlugin.getProvidingPlugin(this.getClass());

    protected ConfigLoader(Class<T> type, String dirName) {
        this(type, new File(CaveCrawlers.getInstance().getDataFolder(), dirName));
    }

    protected ConfigLoader(Class<T> type, File fileDir) {
        this.type = type;
        this.fileDir = fileDir;
        this.itemIDFileMap = new HashMap<>();
    }

    public void load(){
        registerItemsFromFolder(fileDir);
    }

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

    public Set<String> registerItemsFromConfig(FileConfiguration configuration) {
        Set<String> registeredItems = new HashSet<>();
        Set<String> keys = configuration.getKeys(false);
        for (String key : keys) {
            T itemInfo = configuration.getObject(key, type);
            if (itemInfo != null) {
                registeredItems.add(key);
                register(key, itemInfo);
            } else {
                CaveCrawlers.getInstance().getLogger().warning("Failed to Load Item: " + key);
            }
        }
        return registeredItems;
    }

    public CustomConfig getConfig(String Id){
        Map<String, File> idFileMap = getItemIDFileMap();
        File file = idFileMap.get(Id);
        if (file == null){
            file = new File(getFileDir(), Id + ".yml");
        }
        return new CustomConfig(file);
    }

    public void update(String Id, T item){
        CustomConfig config = getConfig(Id);
        config.set(Id, item);
        config.save();
    }

    public void clear(){
        itemIDFileMap.clear();
    }

    public abstract void register(String key, T value);
}
