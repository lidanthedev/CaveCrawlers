package me.lidan.cavecrawlers;

import me.lidan.cavecrawlers.commands.CaveTestCommand;
import me.lidan.cavecrawlers.commands.PotionCommands;
import me.lidan.cavecrawlers.commands.StatCommand;
import me.lidan.cavecrawlers.events.*;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsLoader;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.abilities.*;
import me.lidan.cavecrawlers.packets.PacketManager;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.events.PotionsListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.CommandHandler;
import revxrsal.commands.bukkit.BukkitCommandHandler;

import java.io.File;

public final class CaveCrawlers extends JavaPlugin {
    public static Economy economy = null;
    public File ITEMS_DIR_FILE;
    private CommandHandler commandHandler;

    @Override
    public void onEnable() {
        // Plugin startup logic
        long start = System.currentTimeMillis();
        ITEMS_DIR_FILE = new File(getDataFolder(), "items");
        commandHandler = BukkitCommandHandler.create(this);
        commandHandler.getAutoCompleter().registerParameterSuggestions(OfflinePlayer.class, (args, sender, command) -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());

        if (!setupEconomy() ) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

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
        abilityManager.registerAbility("ERROR_BOOM", new BoomAbility(1000, 5));
        abilityManager.registerAbility("BASIC_LASER", new LaserAbility("Laser", "Shoot Laser", 10, 100, Particle.END_ROD, 100, 0.1, 16));
        abilityManager.registerAbility("ZOMBIE_SWORD_HEAL", new InstantHealAbility(70, 4, 5000, 120, 5));
        abilityManager.registerAbility("TARGET_MOB", new TargetAbility("BATTLE", "makes mobs battle", 0, 100));
        abilityManager.registerAbility("HURRICANE_SHOT", new MultiShotAbility(5));
        abilityManager.registerAbility("FURY_SHOT", new MultiShotAbility(3, 1000, 3, 4));
        abilityManager.registerAbility("DOUBLE_SHOT", new MultiShotAbility(2, 1000, 3, 4));
    }

    public void registerItems() {
        ItemsManager itemsManager = ItemsManager.getInstance();
        ItemsLoader itemsLoader = ItemsLoader.getInstance();
        itemsManager.registerExampleItems();
        itemsLoader.registerItemsFromFolder(ITEMS_DIR_FILE);
    }

    public void registerCommands(){
        commandHandler.getAutoCompleter().registerParameterSuggestions(StatType.class, (args, sender, command) -> StatType.names());
        commandHandler.register(new StatCommand());
        commandHandler.register(new CaveTestCommand(commandHandler));
        commandHandler.register(new PotionCommands());
    }

    public void registerEvents(){
        registerEvent(new AntiBanListener());
        registerEvent(new DamageEntityListener());
        registerEvent(new RemoveArrowsListener());
        registerEvent(new ItemChangeListener());
        registerEvent(new UpdateItemsListener());
        registerEvent(new PotionsListener(200));
        registerEvent(new AntiExplodeListener());
        registerEvent(new AntiPlaceListener());
        PacketManager.getInstance().cancelDamageIndicatorParticle();
    }

    public void startTasks(){
        getServer().getScheduler().runTaskTimer(this, bukkitTask -> {
            StatsManager.getInstance().statLoop();
        }, 0, 20);
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

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return true;
    }

    public static CaveCrawlers getInstance(){
        return CaveCrawlers.getPlugin(CaveCrawlers.class);
    }
}
