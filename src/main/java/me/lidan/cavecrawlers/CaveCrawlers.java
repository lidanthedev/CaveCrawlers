package me.lidan.cavecrawlers;

import dev.triumphteam.gui.guis.BaseGui;
import io.lumine.mythic.bukkit.MythicBukkit;
import lombok.Getter;
import me.lidan.cavecrawlers.bosses.BossDrop;
import me.lidan.cavecrawlers.bosses.BossDrops;
import me.lidan.cavecrawlers.bosses.BossLoader;
import me.lidan.cavecrawlers.commands.*;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.DropLoader;
import me.lidan.cavecrawlers.drops.EntityDrops;
import me.lidan.cavecrawlers.drops.SimpleDrop;
import me.lidan.cavecrawlers.griffin.GriffinDrop;
import me.lidan.cavecrawlers.griffin.GriffinDrops;
import me.lidan.cavecrawlers.griffin.GriffinLoader;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemType;
import me.lidan.cavecrawlers.items.ItemsLoader;
import me.lidan.cavecrawlers.items.Rarity;
import me.lidan.cavecrawlers.items.abilities.*;
import me.lidan.cavecrawlers.listeners.*;
import me.lidan.cavecrawlers.mining.BlockInfo;
import me.lidan.cavecrawlers.mining.BlockLoader;
import me.lidan.cavecrawlers.mining.MiningManager;
import me.lidan.cavecrawlers.objects.CaveCrawlersExpansion;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import me.lidan.cavecrawlers.objects.SoundOptions;
import me.lidan.cavecrawlers.objects.TitleOptions;
import me.lidan.cavecrawlers.packets.PacketManager;
import me.lidan.cavecrawlers.perks.Perk;
import me.lidan.cavecrawlers.perks.PerksLoader;
import me.lidan.cavecrawlers.shop.ShopLoader;
import me.lidan.cavecrawlers.shop.ShopMenu;
import me.lidan.cavecrawlers.skills.Skill;
import me.lidan.cavecrawlers.skills.SkillType;
import me.lidan.cavecrawlers.skills.Skills;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.storage.PlayerDataManager;
import me.lidan.cavecrawlers.utils.Cuboid;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import revxrsal.commands.bukkit.BukkitCommandHandler;

import java.util.Arrays;

@Getter
public final class CaveCrawlers extends JavaPlugin {
    public static Economy economy = null;
    private BukkitCommandHandler commandHandler;
    private MythicBukkit mythicBukkit;
    private CaveCrawlersExpansion caveCrawlersExpansion;

    @Override
    public void onEnable() {
        // Plugin startup logic
        long start = System.currentTimeMillis();
        commandHandler = BukkitCommandHandler.create(this);
        commandHandler.getAutoCompleter().registerParameterSuggestions(OfflinePlayer.class, (args, sender, command) -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        commandHandler.getAutoCompleter().registerParameterSuggestions(Sound.class, (args, sender, command) -> {
            return Arrays.stream(Sound.values()).map(Enum::name).toList();
        });
        commandHandler.getAutoCompleter().registerParameterSuggestions(Material.class, (args, sender, command) -> {
            return Arrays.stream(Material.values()).map(Enum::name).toList();
        });
        commandHandler.getAutoCompleter().registerParameterSuggestions(ItemType.class, (args, sender, command) -> {
            return Arrays.stream(ItemType.values()).map(Enum::name).toList();
        });
        commandHandler.getAutoCompleter().registerParameterSuggestions(Rarity.class, (args, sender, command) -> {
            return Arrays.stream(Rarity.values()).map(Enum::name).toList();
        });

        if (!setupEconomy() ) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (getServer().getPluginManager().getPlugin("MythicMobs") != null) {
            mythicBukkit = MythicBukkit.inst();
        }
        registerSerializer();

        registerConfig();

        registerAbilities();
        registerItems();
        registerShops();
        registerBlocks();
        registerDrops();
        registerBosses();
        registerGriffin();
        registerPerks();

        registerCommands();
        registerEvents();
        registerPlaceholders();

        startTasks();

        StatsManager.getInstance().loadAllPlayers();

        long diff = System.currentTimeMillis() - start;
        getLogger().info("Loaded CaveCrawlers! Took " + diff + "ms");
    }

    private void registerBosses() {
        BossLoader.getInstance().load();
    }

    private void registerConfig() {
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        Skill.setXpToLevelList(getConfig().getDoubleList("skill-need-xp"));
    }

    private void registerDrops() {
        DropLoader.getInstance().load();
    }

    private void registerBlocks() {
        BlockLoader.getInstance().load();
    }

    private void registerGriffin() {
        GriffinLoader.getInstance().load();
    }

    private void registerPerks() {
        PerksLoader.getInstance().load();
    }

    private static void registerSerializer() {
        ConfigurationSerialization.registerClass(Stats.class);
        ConfigurationSerialization.registerClass(ItemInfo.class);
        ConfigurationSerialization.registerClass(ShopMenu.class);
        ConfigurationSerialization.registerClass(BlockInfo.class);
        ConfigurationSerialization.registerClass(SimpleDrop.class);
        ConfigurationSerialization.registerClass(EntityDrops.class);
        ConfigurationSerialization.registerClass(Cuboid.class);
        ConfigurationSerialization.registerClass(Skill.class);
        ConfigurationSerialization.registerClass(Skills.class);
        ConfigurationSerialization.registerClass(GriffinDrop.class);
        ConfigurationSerialization.registerClass(GriffinDrops.class);
        ConfigurationSerialization.registerClass(BossDrop.class);
        ConfigurationSerialization.registerClass(BossDrops.class);
        ConfigurationSerialization.registerClass(Perk.class);
        ConfigurationSerialization.registerClass(ConfigMessage.class);
        ConfigurationSerialization.registerClass(SoundOptions.class);
        ConfigurationSerialization.registerClass(TitleOptions.class);
        ConfigurationSerialization.registerClass(Drop.class);
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
        abilityManager.registerAbility("SHIELD_THROW", new MidasAbility(10000, 2));
        abilityManager.registerAbility("SHORT_BOW", new ShortBowAbility());
        abilityManager.registerAbility("MULTI_SHORT_BOW", new ShortMultiShotAbility(3));
        abilityManager.registerAbility("TRANSMISSION", new TransmissionAbility(8));
        abilityManager.registerAbility("SPADE", new SpadeAbility());
        abilityManager.registerAbility("GOLDEN_LASER", new GoldenLaserAbility());
        abilityManager.registerAbility("PORTABLE_SHOP", new PortableShopAbility());
        abilityManager.registerAbility("SUPER_PORTABLE_SHOP", new AutoPortableShopAbility());
        abilityManager.registerAbility("FREEZE", new FreezeAbility());
        abilityManager.registerAbility("MYTHIC_SKILL", new MythicSkillAbility("SummonSkeletons"));
        abilityManager.registerAbility("HULK", new HulkAbility());
        abilityManager.registerAbility("POTION", new PotionAbility("Potion", "Edit this!", 10, 1000, 1, 1, PotionEffectType.GLOWING, 10, "players"));
        abilityManager.registerAbility("LIGHTNING", new LightningRodAbility());
        abilityManager.registerAbility("SPIRIT_SPECTRE", new SpiritSpectreAbility());
        abilityManager.registerAbility("ETHER_TRANSMISSION", new EtherTransmissionAbility(12));
        abilityManager.registerAbility("FIRE_SPIRAL", new FireSpiralAbility());
        abilityManager.registerAbility("EARTH_SHOOTER", new EarthShooterAbility());
        abilityManager.registerAbility("REAPER_IMPACT", new SoulReaperAbility(1000, 5, 10));
    }

    public void registerItems() {
        ItemsLoader itemsLoader = ItemsLoader.getInstance();
        itemsLoader.load();
    }

    public void registerShops(){
        ShopLoader shopLoader = ShopLoader.getInstance();
        shopLoader.load();
    }

    public void registerCommands(){
        commandHandler.getAutoCompleter().registerParameterSuggestions(StatType.class, (args, sender, command) -> StatType.names());
        commandHandler.getAutoCompleter().registerParameterSuggestions(SkillType.class, (args, sender, command) -> SkillType.names());
        commandHandler.register(new StatCommand());
        commandHandler.register(new CaveTestCommand(commandHandler));
        commandHandler.register(new PotionCommands());
        commandHandler.register(new SkillCommand());
        commandHandler.register(new QolCommand());
        commandHandler.register(new MenuCommands());
        commandHandler.register(new SellCommand());
        commandHandler.registerBrigadier();
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
        registerEvent(new MiningListener());
        registerEvent(new EntityDeathListener());
        registerEvent(new XpGainingListener());
        registerEvent(new MenuItemListener());
        registerEvent(new EntityChangeBlockListener());
        registerEvent(new GriffinListener());
        registerEvent(new WorldChangeListener());
        registerEvent(new InfoclickListener());
        registerEvent(new RightClickPlayerViewer());
        registerEvent(new AntiStupidStuffListener());
        registerEvent(new PerksListener());
        registerEvent(new FirstJoinListener());
        PacketManager.getInstance().cancelDamageIndicatorParticle();
    }

    public void registerPlaceholders() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            caveCrawlersExpansion = new CaveCrawlersExpansion(this);
            caveCrawlersExpansion.register();
            ConfigMessage.usePlaceholderAPI = true;
        }
    }

    public void startTasks(){
        getServer().getScheduler().runTaskTimer(this, bukkitTask -> {
            StatsManager.getInstance().statLoop();
            Bukkit.getOnlinePlayers().forEach(player -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 1, true, false, false));
            });
        }, 0, 20);
    }

    public void registerEvent(Listener listener){
        getServer().getPluginManager().registerEvents(listener, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getServer().getScheduler().cancelTasks(this);
        MiningManager.getInstance().regenBlocks();
        PlayerDataManager.getInstance().saveAll();
        killEntities();
        closeAllGuis();
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            caveCrawlersExpansion.unregister();
        }
    }

    private void closeAllGuis() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof BaseGui) {
                player.closeInventory();
            }
        });
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
