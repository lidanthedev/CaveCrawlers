package me.lidan.cavecrawlers.integration;

import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.CaveCrawlers;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MythicMobsHook {
    private static MythicMobsHook instance;
    private static final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private final MythicBukkit mythicBukkit = plugin.getMythicBukkit();
    private final BukkitAPIHelper mythicAPIHelper = mythicBukkit.getAPIHelper();
    private final Map<String, MythicMob> reverseMobNameCache = new HashMap<>();

    public Entity spawnMythicMob(String mob, Location location) {
        try {
            return mythicAPIHelper.spawnMythicMob(mob, location);
        } catch (InvalidMobTypeException e) {
            plugin.getLogger().warning("Failed to spawn mobs %s at %s".formatted(mob, location));
        }
        return null;
    }

    public MythicMob getMobByName(String name) {
        name = ChatColor.translateAlternateColorCodes('&', name);
        return reverseMobNameCache.computeIfAbsent(name, mobName -> {
            for (MythicMob mob : plugin.getMythicBukkit().getMobManager().getMobTypes()) {
                if (mob.getDisplayName() != null && mob.getDisplayName().isPresent()) {
                    if (mob.getDisplayName().get().equalsIgnoreCase(mobName))
                        return mob;
                }
            }
            return null;
        });
    }

    public @Nullable String getMobNameByID(String id) {
        MythicMob mob = plugin.getMythicBukkit().getAPIHelper().getMythicMob(id);
        if (mob == null || !mob.getDisplayName().isPresent()) {
            return null;
        }
        return mob.getDisplayName().get();
    }

    public static MythicMobsHook getInstance() {
        if (instance == null) {
            instance = new MythicMobsHook();
            if (instance.mythicBukkit == null) {
                log.error("MythicBukkit is null - MythicMobsHook will not work");
            }
        }
        return instance;
    }
}
