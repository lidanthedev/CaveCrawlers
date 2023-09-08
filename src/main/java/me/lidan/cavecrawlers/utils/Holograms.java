package me.lidan.cavecrawlers.utils;

import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

public class Holograms {
    /**
     * Spawns a temporary armor stand with text at the specified location.
     *
     * @param location The location to spawn the armor stand.
     * @param text The text to display on the armor stand.
     * @param delay The delay in ticks before the armor stand is removed.
     */
    public static void spawnTempArmorStand(Location location, String text, int delay) {
        ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);

        armorStand.addScoreboardTag("damageHolo");
        armorStand.setCustomName(text);
        armorStand.setCustomNameVisible(true);
        armorStand.setInvulnerable(true);
        armorStand.setVisible(false);
        armorStand.setGravity(false);

        Bukkit.getScheduler().runTaskLater(CaveCrawlers.getInstance(), armorStand::remove, delay);
    }

}
