package me.lidan.cavecrawlers;

import me.lidan.cavecrawlers.commands.StatCommand;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.CommandHandler;
import revxrsal.commands.bukkit.BukkitCommandHandler;

public final class CaveCrawlers extends JavaPlugin {

    private CommandHandler commandHandler;

    @Override
    public void onEnable() {
        // Plugin startup logic
        commandHandler = BukkitCommandHandler.create(this);

    }

    public void registerCommands(){
        commandHandler.register(new StatCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static CaveCrawlers getInstance(){
        return CaveCrawlers.getPlugin(CaveCrawlers.class);
    }
}
