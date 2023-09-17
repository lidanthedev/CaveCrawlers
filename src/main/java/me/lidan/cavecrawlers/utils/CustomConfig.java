package me.lidan.cavecrawlers.utils;

import lombok.Getter;
import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

@Getter
public class CustomConfig extends YamlConfiguration {
    private final File file;

    public CustomConfig(File file) {
        this.file = file;
        this.setup();
        this.load();
    }

    public CustomConfig(String name){
        this(new File(CaveCrawlers.getInstance().getDataFolder(), name + (name.contains(".yml") ? "" : ".yml")));
    }

    public void setup(){
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                CaveCrawlers.getInstance().getLogger().warning("Could not create " + file.getName());
                e.printStackTrace();
            }
        }
        this.load();
    }

    public void save() {
        try {
            this.save(file);
        } catch (IOException e) {
            System.out.println("Couldn't save file " + file.getName());
            e.printStackTrace();
        }
    }

    public CustomConfig load() {
        try {
            this.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            CaveCrawlers.getInstance().getLogger().warning("Failed to load CustomConfig " + file.getName());
            e.printStackTrace();
        }
        return this;
    }
}
