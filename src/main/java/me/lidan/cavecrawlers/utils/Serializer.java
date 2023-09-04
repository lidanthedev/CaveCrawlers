package me.lidan.cavecrawlers.utils;

import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class Serializer {
    public static String serialize(Object object) {
        try {
            ByteArrayOutputStream io = new ByteArrayOutputStream();
            BukkitObjectOutputStream os = new BukkitObjectOutputStream(io);
            os.writeObject(object);
            os.flush();

            byte[] serialized = io.toByteArray();

            return Base64.getEncoder().encodeToString(serialized);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static <T> T deserialize(String object) {
        try {
            byte[] deserialized = Base64.getDecoder().decode(object);

            ByteArrayInputStream in = new ByteArrayInputStream(deserialized);
            BukkitObjectInputStream is = new BukkitObjectInputStream(in);

            return (T) is.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            CaveCrawlers.getInstance().getLogger().info("Failed to read object: " + object);
            ex.printStackTrace();
        }
        return null;
    }
}
