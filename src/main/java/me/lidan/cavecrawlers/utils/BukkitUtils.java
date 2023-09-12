package me.lidan.cavecrawlers.utils;

import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class BukkitUtils {
    public static Vector getVector(Entity player, double yawDegrees, double pitchDegrees, double multiplayer) {
        Vector vector = new Vector();

        double rotX = player.getLocation().getYaw() + yawDegrees;
        double rotY = player.getLocation().getPitch() + pitchDegrees;

        vector.setY(-Math.sin(Math.toRadians(rotY)));

        double xz = Math.cos(Math.toRadians(rotY));

        vector.setX(-xz * Math.sin(Math.toRadians(rotX)));
        vector.setZ(xz * Math.cos(Math.toRadians(rotX)));

        return vector.multiply(multiplayer);
    }
}
