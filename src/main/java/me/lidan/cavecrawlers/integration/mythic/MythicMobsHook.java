package me.lidan.cavecrawlers.integration.mythic;

import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicDropLoadEvent;
import io.lumine.mythic.bukkit.events.MythicReloadEvent;
import io.lumine.mythic.core.items.ItemExecutor;
import io.lumine.mythic.core.items.MythicItem;
import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
public class MythicMobsHook implements Listener {
    public static final String EXPERIMENTAL_MYTHICMOBS_ITEMS_SUPPLIER = "experimental.mythicmobs-items-supplier";
    private static MythicMobsHook instance;
    private static final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private final MythicBukkit mythicBukkit;
    private final BukkitAPIHelper mythicAPIHelper;
    private final Map<String, MythicMob> reverseMobNameCache = new HashMap<>();

    private MythicMobsHook() {
        this.mythicBukkit = plugin.getMythicBukkit();
        if (mythicBukkit == null) {
            log.error("MythicBukkit is null - MythicMobsHook will not work");
            this.mythicAPIHelper = null;
            return;
        }
        this.mythicAPIHelper = mythicBukkit.getAPIHelper();
    }

    public void load() {
        reverseMobNameCache.clear();
        tryRegisterItemSuppliers();
    }

    private void tryRegisterItemSuppliers() {
        if (!plugin.getConfig().getBoolean(EXPERIMENTAL_MYTHICMOBS_ITEMS_SUPPLIER, false)) return;
        try {
            registerItemSupplier();
            registerItemSupplierLegacy();
        } catch (Exception e) {
            log.error("Failed to register MythicMobs item suppliers", e);
        }
    }

    public void registerItemSupplier() {
        mythicBukkit.getItemManager().registerItemSupplier(new MythicItemSupport());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMythicReloaded(MythicReloadEvent event) {
        load();
    }

    @EventHandler(ignoreCancelled = true)
    public void onMythicDropLoad(MythicDropLoadEvent event) {
        if (!plugin.getConfig().getBoolean(EXPERIMENTAL_MYTHICMOBS_ITEMS_SUPPLIER, false)) return;
        if (!event.getDropName().equalsIgnoreCase("cavecrawlers")) return;
        ItemInfo itemInfo = ItemsManager.getInstance().getItemByID(event.getArgument());
        if (itemInfo == null) return;
        event.register(new MythicCaveDrop(itemInfo));
    }

    // note: ItemSupplier api still WIP
    public void registerItemSupplierLegacy() {
        Set<String> keys = ItemsManager.getInstance().getKeys();
        for (String key : keys) {
            String internalName = "cavecrawlers:" + key;
            ItemInfo itemInfo = ItemsManager.getInstance().getItemByID(key);
            if (itemInfo == null) continue;
            try {
                MythicCaveItem item = new MythicCaveItem(key, itemInfo);
                registerMythicItemForce(internalName, item);
            } catch (Exception e) {
                log.error("registerItemSupplierLegacy: Failed to register item {}", key, e);
            }
        }
    }

    public void registerMythicItemForce(String key, MythicItem item) {
        Class<ItemExecutor> itemExecutorClass = ItemExecutor.class;
        try {
            Field itemField = itemExecutorClass.getDeclaredField("items");
            itemField.setAccessible(true);
            Map<String, MythicItem> items = (Map<String, MythicItem>) itemField.get(mythicBukkit.getItemManager());
            items.put(key, item);
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            log.error("registerMythicItemForce: Failed to register item force {}", key, e);
        }
    }

    public @Nullable Entity spawnMythicMob(String mob, Location location) {
        if (mythicAPIHelper == null) return null;
        try {
            return mythicAPIHelper.spawnMythicMob(mob, location);
        } catch (InvalidMobTypeException e) {
            plugin.getLogger().warning("Failed to spawn mobs %s at %s".formatted(mob, location));
        }
        return null;
    }

    public MythicMob getMobByName(String name) {
        if (mythicBukkit == null) return null;
        name = ChatColor.translateAlternateColorCodes('&', name);
        return reverseMobNameCache.computeIfAbsent(name, mobName -> {
            for (MythicMob mob : mythicBukkit.getMobManager().getMobTypes()) {
                if (mob.getDisplayName() != null && mob.getDisplayName().isPresent()) {
                    if (mob.getDisplayName().get().equalsIgnoreCase(mobName))
                        return mob;
                }
            }
            return null;
        });
    }

    public @Nullable String getMobNameByID(String id) {
        if (mythicBukkit == null) return null;
        MythicMob mob = mythicBukkit.getAPIHelper().getMythicMob(id);
        if (mob == null || !mob.getDisplayName().isPresent()) {
            return null;
        }
        return mob.getDisplayName().get();
    }

    public static MythicMobsHook getInstance() {
        if (instance == null) {
            instance = new MythicMobsHook();
        }
        return instance;
    }
}
