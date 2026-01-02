package me.lidan.cavecrawlers;

import com.cryptomorin.xseries.XSound;
import dev.triumphteam.gui.guis.BaseGui;
import io.lumine.mythic.bukkit.MythicBukkit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.altar.Altar;
import me.lidan.cavecrawlers.altar.AltarDrop;
import me.lidan.cavecrawlers.altar.AltarLoader;
import me.lidan.cavecrawlers.altar.AltarManager;
import me.lidan.cavecrawlers.api.*;
import me.lidan.cavecrawlers.bosses.BossDrop;
import me.lidan.cavecrawlers.bosses.BossDrops;
import me.lidan.cavecrawlers.bosses.BossLoader;
import me.lidan.cavecrawlers.bosses.BossManager;
import me.lidan.cavecrawlers.commands.*;
import me.lidan.cavecrawlers.damage.DamageManager;
import me.lidan.cavecrawlers.drops.*;
import me.lidan.cavecrawlers.entities.EntityManager;
import me.lidan.cavecrawlers.integration.CaveCrawlersExpansion;
import me.lidan.cavecrawlers.items.*;
import me.lidan.cavecrawlers.items.abilities.*;
import me.lidan.cavecrawlers.levels.LevelConfigManager;
import me.lidan.cavecrawlers.listeners.*;
import me.lidan.cavecrawlers.mining.BlockInfo;
import me.lidan.cavecrawlers.mining.BlockLoader;
import me.lidan.cavecrawlers.mining.MiningManager;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import me.lidan.cavecrawlers.objects.SoundOptions;
import me.lidan.cavecrawlers.objects.TitleOptions;
import me.lidan.cavecrawlers.packets.PacketManager;
import me.lidan.cavecrawlers.perks.Perk;
import me.lidan.cavecrawlers.perks.PerksLoader;
import me.lidan.cavecrawlers.prompt.PromptManager;
import me.lidan.cavecrawlers.shop.ShopLoader;
import me.lidan.cavecrawlers.shop.ShopMenu;
import me.lidan.cavecrawlers.skills.*;
import me.lidan.cavecrawlers.stats.ActionBarManager;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.storage.PlayerDataManager;
import me.lidan.cavecrawlers.utils.Cuboid;
import me.lidan.cavecrawlers.utils.Holograms;
import net.md_5.bungee.api.ChatColor;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

@Slf4j
@Getter
public final class CaveCrawlers extends JavaPlugin implements CaveCrawlersAPI {
    public static final int TICKS_TO_SECOND = 20;
    public static Economy economy = null;
    private BukkitCommandHandler commandHandler;
    private MythicBukkit mythicBukkit;
    private CaveCrawlersExpansion caveCrawlersExpansion;

    /**
     * Runs when the plugin is enabled
     */
    @Override
    public void onEnable() {
        // Plugin startup logic
        long start = System.currentTimeMillis();
        commandHandler = BukkitCommandHandler.create(this);
        if (!setupEconomy()) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (getServer().getPluginManager().getPlugin("MythicMobs") != null) {
            mythicBukkit = MythicBukkit.inst();
        }
        registerSerializer();

        saveDefaultResources();
        registerConfig();

        registerAbilities();
        registerFromConfigs(this);
        registerSkills();
        registerLevels();

        registerCommandResolvers();
        registerCommandCompletions();
        registerCommands();
        registerEvents();
        registerPlaceholders();

        startTasks();

        StatsManager.getInstance().loadAllPlayers();

        long diff = System.currentTimeMillis() - start;
        getLogger().info("Loaded CaveCrawlers! Took " + diff + "ms");
    }

    @Override
    public void registerFromConfigs(JavaPlugin plugin) {
        registerItems(plugin);
        registerShops(plugin);
        registerBlocks(plugin);
        registerDrops(plugin);
        registerBosses(plugin);
        registerPerks(plugin);
        registerAltars(plugin);
    }

    /**
     * Save default resources
     */
    private void saveDefaultResources() {
        if (getDataFolder().exists()) {
            return;
        }
        log.info("Detected first time setup, saving default resources");
        getDataFolder().mkdir();
        saveResource("example-skills.yml", new File(getDataFolder(), "skills/example-skills.yml"));
        saveResource("example-items.yml", new File(getDataFolder(), "items/example-items.yml"));
        saveResource("messages.yml", false);
        saveResource("example-shop.yml", new File(getDataFolder(), "shops/example-shop.yml"));
    }

    /**
     * Register skills
     */
    private void registerSkills() {
        SkillsManager.getInstance().load();
    }

    /**
     * Register bosses
     */
    private void registerBosses(JavaPlugin plugin) {
        BossLoader.getInstance().load(plugin.getDataFolder());
    }

    /**
     * Register config
     */
    private void registerConfig() {
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        Skill.setDefaultXpToLevelList(getConfig().getDoubleList("skill-need-xp"));
    }

    /**
     * Register drops
     */
    private void registerDrops(JavaPlugin plugin) {
        DropLoader.getInstance().load(plugin.getDataFolder());
    }

    /**
     * Register blocks
     * Used by the custom mining system
     */
    private void registerBlocks(JavaPlugin plugin) {
        BlockLoader.getInstance().load(plugin.getDataFolder());
    }

    /**
     * Register perks
     */
    private void registerPerks(JavaPlugin plugin) {
        PerksLoader.getInstance().load(plugin.getDataFolder());
    }

    /**
     * Register levels
     */
    private void registerLevels() {
        LevelConfigManager.getInstance().saveDefaultConfig();
    }

    /**
     * Register serializer
     */
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
        ConfigurationSerialization.registerClass(BossDrop.class);
        ConfigurationSerialization.registerClass(BossDrops.class);
        ConfigurationSerialization.registerClass(Perk.class);
        ConfigurationSerialization.registerClass(ConfigMessage.class);
        ConfigurationSerialization.registerClass(SoundOptions.class);
        ConfigurationSerialization.registerClass(TitleOptions.class);
        ConfigurationSerialization.registerClass(Drop.class);
        ConfigurationSerialization.registerClass(AltarDrop.class);
        ConfigurationSerialization.registerClass(Altar.class);
        ConfigurationSerialization.registerClass(CoinSkillReward.class);
        ConfigurationSerialization.registerClass(ItemSkillReward.class);
        ConfigurationSerialization.registerClass(StatSkillReward.class);
        ConfigurationSerialization.registerClass(SkillInfo.class);
    }

    /**
     * Register abilities
     */
    private void registerAbilities() {
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
        abilityManager.registerAbility("MIDAS_STAFF", new MidasAbility(10000, 2));
        abilityManager.registerAbility("SHORT_BOW", new ShortBowAbility());
        abilityManager.registerAbility("MULTI_SHORT_BOW", new ShortMultiShotAbility(3));
        abilityManager.registerAbility("TRANSMISSION", new TransmissionAbility(8));
        abilityManager.registerAbility("GOLDEN_LASER", new GoldenLaserAbility());
        abilityManager.registerAbility("PORTABLE_SHOP", new PortableShopAbility());
        abilityManager.registerAbility("SUPER_PORTABLE_SHOP", new AutoPortableShopAbility());
        abilityManager.registerAbility("AUTO_PORTABLE_SHOP", new AutoPortableShopAbility());
        abilityManager.registerAbility("AUTO_FULL_SHOP", new AutoFullShopAbility());
        abilityManager.registerAbility("FREEZE", new FreezeAbility());
        if (mythicBukkit != null) {
            abilityManager.registerAbility("MYTHIC_SKILL", new MythicSkillAbility("SummonSkeletons"));
        }
        abilityManager.registerAbility("HULK", new HulkAbility());
        abilityManager.registerAbility("POTION", new PotionAbility("Potion", "Edit this!", 10, 1000, 1, 1, PotionEffectType.GLOWING, 10, "players"));
        abilityManager.registerAbility("LIGHTNING", new LightningRodAbility());
        abilityManager.registerAbility("SPIRIT_SPECTRE", new SpiritSpectreAbility());
        abilityManager.registerAbility("ETHER_TRANSMISSION", new EtherTransmissionAbility(12));
        abilityManager.registerAbility("FIRE_SPIRAL", new FireSpiralAbility());
        abilityManager.registerAbility("EARTH_SHOOTER", new EarthShooterAbility());
        abilityManager.registerAbility("REAPER_IMPACT", new SoulReaperAbility(1000, 5, 10));
        abilityManager.registerAbility("ARROW_SPIRAL", new ArrowSpiralAbility());
    }

    /**
     * Register items
     */
    public void registerItems(JavaPlugin plugin) {
        ItemsLoader itemsLoader = ItemsLoader.getInstance();
        itemsLoader.load(plugin.getDataFolder());
    }

    /**
     * Register shops
     */
    public void registerShops(JavaPlugin plugin) {
        ShopLoader shopLoader = ShopLoader.getInstance();
        shopLoader.load(plugin.getDataFolder());
    }

    /**
     * Register altars
     */
    public void registerAltars(JavaPlugin plugin) {
        AltarLoader altarLoader = AltarLoader.getInstance();
        altarLoader.load(plugin.getDataFolder());
    }

    /**
     * Register commands
     */
    public void registerCommands() {
        commandHandler.register(new StatCommand());
        commandHandler.register(new CaveCrawlersMainCommand(commandHandler));
        commandHandler.register(new SkillCommand());
        commandHandler.register(new QolCommand());
        commandHandler.register(new MenuCommands());
        commandHandler.register(new SellCommand());
        commandHandler.registerBrigadier();
    }

    /**
     * Register command resolvers
     */
    private void registerCommandResolvers() {
        commandHandler.registerValueResolver(Altar.class, valueResolverContext -> {
            return AltarManager.getInstance().getAltar(valueResolverContext.pop());
        });
        commandHandler.registerValueResolver(ChatColor.class, valueResolverContext -> {
            return ChatColor.valueOf(valueResolverContext.pop());
        });
        commandHandler.registerValueResolver(SkillInfo.class, valueResolverContext -> {
            return SkillsManager.getInstance().getSkillInfo(valueResolverContext.pop());
        });
        commandHandler.registerValueResolver(StatType.class, valueResolverContext -> {
            return StatType.valueOf(valueResolverContext.pop());
        });
        commandHandler.registerValueResolver(Sound.class, valueResolverContext -> {
            return Registry.SOUNDS.get(NamespacedKey.minecraft(valueResolverContext.pop()));
        });
    }

    /**
     * Register command completions
     */
    private void registerCommandCompletions() {
        commandHandler.getAutoCompleter().registerParameterSuggestions(OfflinePlayer.class, (args, sender, command) -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        commandHandler.getAutoCompleter().registerParameterSuggestions(Sound.class, (args, sender, command) -> {
            return XSound.getValues().stream().map(sound -> sound.get().toString()).toList();
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
        commandHandler.getAutoCompleter().registerParameterSuggestions(Altar.class, (args, sender, command) -> {
            return AltarManager.getInstance().getAltarNames();
        });
        commandHandler.getAutoCompleter().registerParameterSuggestions(StatType.class, (args, sender, command) -> StatType.names());
        commandHandler.getAutoCompleter().registerParameterSuggestions(SkillInfo.class, (args, sender, command) -> SkillsManager.getInstance().getSkillInfoMap().keySet());
    }

    /**
     * Register events
     */
    public void registerEvents() {
        registerEvent(new DamageEntityListener());
        registerEvent(new RemoveArrowsListener());
        registerEvent(new ItemChangeListener());
        registerEvent(new UpdateItemsListener());
        registerEvent(new AntiExplodeListener());
        registerEvent(new AntiPlaceListener());
        registerEvent(new MiningListener());
        registerEvent(new EntityDeathListener());
        registerEvent(new SkillXpGainingListener());
        registerEvent(new MenuItemListener());
        registerEvent(new EntityChangeBlockListener());
        registerEvent(new WorldChangeListener());
        registerEvent(new InfoclickListener());
        registerEvent(new RightClickPlayerViewer());
        registerEvent(new AntiStupidStuffListener());
        registerEvent(new PerksListener());
        registerEvent(new FirstJoinListener(this));
        registerEvent(new AltarListener());
        registerEvent(new ChatPromptListener());
        PacketManager.getInstance().cancelDamageIndicatorParticle();
    }

    /**
     * Register placeholders
     */
    public void registerPlaceholders() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            caveCrawlersExpansion = new CaveCrawlersExpansion(this);
            caveCrawlersExpansion.register();
            ConfigMessage.usePlaceholderAPI = true;
        }
    }

    /**
     * Start tasks
     */
    public void startTasks() {
        getServer().getScheduler().runTaskTimer(this, bukkitTask -> {
            StatsManager.getInstance().statLoop();
            if (getConfig().getBoolean("night-vision")) {
                Bukkit.getOnlinePlayers().forEach(player -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 1, true, false, false));
                });
            }
            ItemsManager.getInstance().loadNotFullyLoadedItems();
        }, 0, TICKS_TO_SECOND);
    }

    /**
     * Register event
     *
     * @param listener the listener to register
     */
    public void registerEvent(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    /**
     * Runs when the plugin is disabled
     */
    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getServer().getScheduler().cancelTasks(this);
        MiningManager.getInstance().regenBlocks();
        PlayerDataManager.getInstance().saveAll();
        AltarManager.getInstance().reset();
        killHolograms();
        closeAllGuis();
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") && caveCrawlersExpansion != null) {
            caveCrawlersExpansion.unregister();
        }
    }

    /**
     * Close all guis
     */
    private void closeAllGuis() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof BaseGui) {
                player.closeInventory();
            }
        });
    }

    /**
     * Kill all holograms
     */
    public void killHolograms() {
        Bukkit.getWorlds().forEach(world -> {
            world.getEntities().forEach(entity -> {
                if (entity.getScoreboardTags().contains(Holograms.HOLOGRAM_TAG)) {
                    entity.remove();
                }
            });
        });
    }

    /**
     * Setup economy
     * @return true if economy is setup
     */
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

    /**
     * Save a resource to a file path
     * Used to save resources to subdirectories in the plugin folder
     * @param resource the resource
     * @param path the path as File object
     */
    public void saveResource(String resource, File path) {
        if (!path.exists()) {
            path.getParentFile().mkdirs();
            try (InputStream in = getResource(resource);
                 FileOutputStream out = new FileOutputStream(path)) {
                if (in == null) {
                    getLogger().warning("Resource not found: " + resource);
                    return;
                }
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static CaveCrawlersAPI getAPI() {
        return CaveCrawlers.getInstance();
    }

    @Override
    public AbilityAPI getAbilityAPI() {
        return AbilityManager.getInstance();
    }

    @Override
    public ActionBarAPI getActionBarAPI() {
        return ActionBarManager.getInstance();
    }

    @Override
    public BossAPI getBossAPI() {
        return BossManager.getInstance();
    }

    @Override
    public DamageAPI getDamageAPI() {
        return DamageManager.getInstance();
    }

    @Override
    public DropsAPI getDropsAPI() {
        return DropsManager.getInstance();
    }

    @Override
    public EntityAPI getEntityAPI() {
        return EntityManager.getInstance();
    }

    @Override
    public ItemsAPI getItemsAPI() {
        return ItemsManager.getInstance();
    }

    @Override
    public MiningAPI getMiningAPI() {
        return MiningManager.getInstance();
    }

    @Override
    public PromptAPI getPromptAPI() {
        return PromptManager.getInstance();
    }

    @Override
    public SkillsAPI getSkillsAPI() {
        return SkillsManager.getInstance();
    }

    /**
     * Get the plugin instance
     * @return the plugin instance
     */
    public static CaveCrawlers getInstance() {
        return CaveCrawlers.getPlugin(CaveCrawlers.class);
    }

    @Override
    public StatsAPI getStatsAPI() {
        return StatsManager.getInstance();
    }
}
