package me.lidan.cavecrawlers.utils;

import dev.dejvokep.boostedyaml.settings.Settings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.spigot.SpigotSerializer;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * BoostedCustomConfig class to manage custom configs with BoostedYAML
 * BoostedCustomConfig is used to create extra configs for the plugin
 * used to store data that is not stored in the main config
 * How to use:
 * Make a new instance of the BoostedCustomConfig class
 * Set the values in the config by calling set(key, value) value must implement ConfigurationSerializable
 * Save the config by calling save()
 * The Config will load automatically
 */
@Getter
public class BoostedCustomConfig extends BoostedConfiguration {
    private static final Logger log = LoggerFactory.getLogger(BoostedCustomConfig.class);
    private static final JavaPlugin plugin = JavaPlugin.getProvidingPlugin(BoostedCustomConfig.class);
    private final File file;

    /**
     * Create a new CustomConfig
     *
     * @param file the file
     */
    public BoostedCustomConfig(File file, Settings... settings) throws IOException {
        super(file, null, getSettingsWithExtensions(settings));
        this.file = file;
    }

    public BoostedCustomConfig(File file) throws IOException {
        this(file, (Settings[]) null);
    }

    /**
     * Create a new CustomConfig
     * The file will be created in the plugin data folder if it doesn't exist
     *
     * @param name the name of the file
     */
    public BoostedCustomConfig(String name) throws IOException {
        this(new File(plugin.getDataFolder(), name + (name.contains(".yml") ? "" : ".yml")));
    }

    private static Settings[] getSettingsWithExtensions(Settings... settings) {
        Settings[] defaultSettings = new Settings[]{GeneralSettings.builder().setSerializer(SpigotSerializer.getInstance()).build(),

        };
        if (settings == null || settings.length == 0) {
            return defaultSettings;
        }
        Settings[] combined = Arrays.copyOf(defaultSettings, defaultSettings.length + settings.length);
        System.arraycopy(settings, 0, combined, defaultSettings.length, settings.length);
        return combined;
    }

    /**
     * Save the config
     *
     * @return
     */
    public boolean save() {
        try {
            this.save(file);
            return true;
        } catch (IOException e) {
            log.warn("Couldn't save file {}", file.getName(), e);
        }
        return false;
    }

    /**
     * Load the config
     * Can also be used to reload the config
     *
     * @return the config
     */
    public BoostedCustomConfig load() {
        try {
            this.reload();
        } catch (IOException e) {
            log.warn("Failed to load BoostedCustomConfig {}", file.getName(), e);
        }
        return this;
    }
}
