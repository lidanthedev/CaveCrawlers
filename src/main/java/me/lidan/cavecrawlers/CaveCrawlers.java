package me.lidan.cavecrawlers;

import me.lidan.cavecrawlers.commands.CaveTestCommand;
import me.lidan.cavecrawlers.commands.StatCommand;
import me.lidan.cavecrawlers.events.*;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsLoader;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.abilities.AbilityManager;
import me.lidan.cavecrawlers.items.abilities.ErrorScytheAbility;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.CommandHandler;
import revxrsal.commands.bukkit.BukkitCommandHandler;

import java.io.File;

public final class CaveCrawlers extends JavaPlugin {

    public File ITEMS_DIR_FILE;
    private CommandHandler commandHandler;

    @Override
    public void onEnable() {
        // Plugin startup logic
        ITEMS_DIR_FILE = new File(getDataFolder(), "items");
        commandHandler = BukkitCommandHandler.create(this);
        long start = System.currentTimeMillis();

        registerSerializer();

        registerAbilities();
        registerItems();

        registerCommands();
        registerEvents();
        startTasks();

        StatsManager.getInstance().loadAllPlayers();

        long diff = System.currentTimeMillis() - start;
        getLogger().info("Loaded CaveCrawlers! Took " + diff + "ms");
    }

    private static void registerSerializer() {
        ConfigurationSerialization.registerClass(Stats.class);
        ConfigurationSerialization.registerClass(ItemInfo.class);
    }

    private void registerAbilities() {
        //example ability
        AbilityManager abilityManager = AbilityManager.getInstance();
        abilityManager.registerAbility("ERROR_SCYTHE_ABILITY", new ErrorScytheAbility());
    }

    private void registerItems() {
        ItemsManager itemsManager = ItemsManager.getInstance();
        ItemsLoader itemsLoader = ItemsLoader.getInstance();
        itemsManager.registerExampleItems();
        itemsLoader.registerItemsFromFolder(ITEMS_DIR_FILE);
    }

    public void registerCommands(){
        commandHandler.getAutoCompleter().registerParameterSuggestions(StatType.class, (args, sender, command) -> StatType.names());
        commandHandler.register(new StatCommand());
        commandHandler.register(new CaveTestCommand(commandHandler));
    }

    public void registerEvents(){
        registerEvent(new AntiBanListener());
        registerEvent(new DamageEntityListener());
        registerEvent(new RemoveArrowsListener());
        registerEvent(new ItemChangeListener());
        registerEvent(new UpdateItemsListener());
    }

    public void startTasks(){
        getServer().getScheduler().runTaskTimer(this, bukkitTask -> {
            StatsManager.getInstance().statLoop();
        }, 0, 40);
    }

    public void registerEvent(Listener listener){
        getServer().getPluginManager().registerEvents(listener, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getServer().getScheduler().cancelTasks(this);
        killEntities();
    }

    public void killEntities(){
        Bukkit.getWorlds().forEach(world -> {
            world.getEntities().forEach(entity -> {
                if (entity.getScoreboardTags().contains("HologramCaveCrawlers")){
                    entity.remove();
                }
            });
        });
    }

    public static CaveCrawlers getInstance(){
        return CaveCrawlers.getPlugin(CaveCrawlers.class);
    }
}
