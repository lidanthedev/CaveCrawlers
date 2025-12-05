package me.lidan.cavecrawlers.integration;

import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

@Slf4j
public class MythicMobsHook {
    private static MythicMobsHook instance;
    private static final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private final MythicBukkit mythicBukkit = plugin.getMythicBukkit();
    private final BukkitAPIHelper mythicAPIHelper = mythicBukkit.getAPIHelper();

    public Entity spawnMythicMob(String mob, Location location) {
        try {
            return mythicAPIHelper.spawnMythicMob(mob, location);
        } catch (InvalidMobTypeException e) {
            plugin.getLogger().warning("Failed to spawn mobs %s at %s".formatted(mob, location));
        }
        return null;
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
