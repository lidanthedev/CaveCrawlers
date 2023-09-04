package me.lidan.cavecrawlers;

import me.lidan.cavecrawlers.commands.CaveTestCommand;
import me.lidan.cavecrawlers.commands.StatCommand;
import me.lidan.cavecrawlers.events.AntiBanListener;
import me.lidan.cavecrawlers.events.DamageEntityListener;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.CommandHandler;
import revxrsal.commands.bukkit.BukkitCommandHandler;

public final class CaveCrawlers extends JavaPlugin {

    private CommandHandler commandHandler;

    @Override
    public void onEnable() {
        // Plugin startup logic
        commandHandler = BukkitCommandHandler.create(this);

        registerSerializer();
        registerCommands();
        registerEvents();
        startTasks();
        getLogger().info("Loaded CaveCrawlers! Ready to cave!");
    }

    private static void registerSerializer() {
        ConfigurationSerialization.registerClass(Stats.class, "stats");
    }

    public void startTasks(){
        getServer().getScheduler().runTaskTimer(this, bukkitTask -> {
            StatsManager.getInstance().statLoop();
        }, 0, 40);
    }

    public void registerCommands(){
        commandHandler.getAutoCompleter().registerParameterSuggestions(StatType.class, (args, sender, command) -> StatType.names());
        commandHandler.register(new StatCommand());
        commandHandler.register(new CaveTestCommand());
    }

    public void registerEvents(){
        registerEvent(new AntiBanListener());
        registerEvent(new DamageEntityListener());
    }

    public void registerEvent(Listener listener){
        getServer().getPluginManager().registerEvents(listener, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getServer().getScheduler().cancelTasks(this);
    }

    public static CaveCrawlers getInstance(){
        return CaveCrawlers.getPlugin(CaveCrawlers.class);
    }
}
