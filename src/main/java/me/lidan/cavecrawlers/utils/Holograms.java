package me.lidan.cavecrawlers.utils;

import me.lidan.cavecrawlers.CaveCrawlers;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Mob;

public class Holograms {

    public static final double Y_OFFSET = 2.5;

    /**
     * Spawns a temporary armor stand with text at the specified location.
     *
     * @param location The location to spawn the armor stand.
     * @param text The text to display on the armor stand.
     * @param delay The delay in ticks before the armor stand is removed.
     */
    public static void spawnTempArmorStand(Location location, String text, int delay) {
        ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class, stand -> {
            stand.addScoreboardTag("HologramCaveCrawlers");
            stand.setCustomName(text);
            stand.setCustomNameVisible(true);
            stand.setInvulnerable(true);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setMarker(true);
        });

        Bukkit.getScheduler().runTaskLater(CaveCrawlers.getInstance(), armorStand::remove, delay);
    }

    /**
     * Shows a damage hologram above the mob.
     *
     * @param mob         The mob to show the hologram above.
     * @param finalDamage The final damage dealt to the mob.
     * @param crit        Whether the damage was a critical hit.
     */
    public static void showDamageHologram(Mob mob, int finalDamage, boolean crit) {
        String prettyDamage = StringUtils.getNumberFormat(finalDamage);

        StringBuilder msg = new StringBuilder();
        String formattedDamage;
        if (crit) {
            msg.append("✧").append(prettyDamage).append("✧");
            formattedDamage = StringUtils.rainbowText(msg.toString());
        } else {
            msg.append(ChatColor.GRAY).append(prettyDamage);
            formattedDamage = msg.toString();
        }

        Location hologram = mob.getLocation();
        double random = RandomUtils.randomDouble(1, 1.5);
        hologram.add(mob.getLocation().getDirection().multiply(random));
        hologram.setY(mob.getLocation().getY() + random + Y_OFFSET);
        hologram.subtract(0, 2, 0);
        spawnTempArmorStand(hologram, formattedDamage, 10);
    }
}
