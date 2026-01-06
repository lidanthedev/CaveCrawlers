package me.lidan.cavecrawlers.objects;

import dev.dejvokep.boostedyaml.settings.Settings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import lombok.Getter;
import lombok.Setter;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.utils.BasicDefaultVersioning;
import me.lidan.cavecrawlers.utils.BoostedConfiguration;
import me.lidan.cavecrawlers.utils.BoostedCustomConfig;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public abstract class ConfigLoader<T extends ConfigurationSerializable> {
    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);
    private static final JavaPlugin plugin = JavaPlugin.getProvidingPlugin(ConfigLoader.class);
    public static final String VERSION_KEY = "version";

    private final Class<T> type;
    @Getter
    private final Map<String, File> configMap;
    @Getter
    private final File fileDir;
    @Getter
    @Setter
    private Settings[] settings;
    private BasicDefaultVersioning versioning;

    protected ConfigLoader(Class<T> type, String dirName) {
        this(type, new File(plugin.getDataFolder(), dirName));
    }

    protected ConfigLoader(Class<T> type, File fileDir) {
        this.type = type;
        this.fileDir = fileDir;
        this.configMap = new HashMap<>();
    }

    public void load(){
        load(fileDir);
    }

    /**
     * Load items from a directory.
     *
     * @param dir the plugin data folder.
     */
    public void load(File dir) {
        if (!dir.exists()) {
            return;
        }
        if (dir != fileDir) {
            dir = new File(dir, fileDir.getName());
        }
        registerItemsFromFolder(dir);
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
        if (!file.getName().endsWith(".yml")) {
            return;
        }
        try {
            BoostedCustomConfig customConfig = openConfig(file);
            Set<String> registered = registerItemsFromConfig(customConfig);
            for (String s : registered) {
                configMap.put(s, file);
            }
        } catch (Exception e) {
            CaveCrawlers.getInstance().getLogger().warning("Failed to Load File: " + file.getPath());
            log.error("Failed to load config file: {}", file.getPath(), e);
        }
    }

    public Set<String> registerItemsFromConfig(BoostedConfiguration configuration) {
        Set<String> registeredItems = new HashSet<>();
        Set<String> keys = configuration.getKeys(false);
        for (String key : keys) {
            if (key.equals(VERSION_KEY)) {
                continue;
            }
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

    public BoostedCustomConfig getConfig(String Id) {
        Map<String, File> idFileMap = getConfigMap();
        File file = idFileMap.get(Id);
        if (file == null){
            file = new File(getFileDir(), Id + ".yml");
        }
        try {
            return openConfig(file);
        } catch (IOException e) {
            log.error("Failed to get config for ID: {}", Id, e);
            throw new RuntimeException(e);
        }
    }

    public void update(String Id, T value) {
        BoostedCustomConfig config = getConfig(Id);
        config.set(Id, value);
        config.save();
    }

    public void remove(String Id) {
        BoostedCustomConfig config = getConfig(Id);
        config.remove(Id);
        config.save();
        configMap.remove(Id);
    }

    public void clear(){
        configMap.clear();
    }

    public void setupMigrations(Consumer<UpdaterSettings.Builder> updaterSettingsSupplier) {
        BasicDefaultVersioning versioning = new BasicDefaultVersioning(VERSION_KEY);
        UpdaterSettings.Builder updaterBuilder = UpdaterSettings.builder().setVersioning(versioning).setKeepAll(true);
        updaterSettingsSupplier.accept(updaterBuilder);
        Settings[] settings = new Settings[]{
                LoaderSettings.builder().setAutoUpdate(true).build(),
                updaterBuilder.build()
        };
        setSettings(settings);
        this.versioning = versioning;
    }

    private BoostedCustomConfig openConfig(File file) throws IOException {
        if (this.versioning != null) {
            return new BoostedCustomConfig(file, versioning.getVirtualDefaults(), settings);
        } else {
            return new BoostedCustomConfig(file, settings);
        }
    }

    public abstract void register(String key, T value);
}
