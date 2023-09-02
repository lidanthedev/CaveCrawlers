package me.lidan.cavecrawlers;

import me.lidan.cavecrawlers.commands.StatCommand;
import me.lidan.cavecrawlers.stats.StatType;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.CommandHandler;
import revxrsal.commands.bukkit.BukkitCommandHandler;

public final class CaveCrawlers extends JavaPlugin {

    private CommandHandler commandHandler;

    @Override
    public void onEnable() {
        // Plugin startup logic
        commandHandler = BukkitCommandHandler.create(this);
        registerCommands();
        getLogger().info("Loaded CaveCrawlers!");
    }

    public void registerCommands(){
        commandHandler.getAutoCompleter().registerParameterSuggestions(StatType.class, (args, sender, command) -> {
            return StatType.names();
        });
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
