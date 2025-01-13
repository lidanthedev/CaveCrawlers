package me.lidan.cavecrawlers.utils;

import lombok.Getter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * CustomConfig class to manage custom configs
 * CustomConfig is used to create extra configs for the plugin
 * used to store data that is not stored in the main config
 * How to use:
 * Make a new instance of the CustomConfig class
 * Set the values in the config
 * Save the config
 * The Config will load automatically
 */
@Getter
public class CustomConfig extends YamlConfiguration {
    private final File file;
    private static final JavaPlugin plugin = JavaPlugin.getProvidingPlugin(CustomConfig.class);

    /**
     * Create a new CustomConfig
     *
     * @param file the file
     */
    public CustomConfig(File file) {
        this.file = file;
        this.setup();
        this.load();
    }

    /**
     * Create a new CustomConfig
     * @param name the name of the file
     */
    public CustomConfig(String name){
        this(new File(plugin.getDataFolder(), name + (name.contains(".yml") ? "" : ".yml")));
    }

    /**
     * Setup the config
     */
    public void setup(){
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create " + file.getName());
                e.printStackTrace();
            }
        }
        this.load();
    }

    /**
     * Save the config
     */
    public void save() {
        try {
            this.save(file);
        } catch (IOException e) {
            System.out.println("Couldn't save file " + file.getName());
            e.printStackTrace();
        }
    }

    /**
     * Load the config
     * Can also be used to reload the config
     * @return the config
     */
    public CustomConfig load() {
        try {
            this.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().warning("Failed to load CustomConfig " + file.getName());
            e.printStackTrace();
        }
        return this;
    }
}
