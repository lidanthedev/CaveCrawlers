package me.lidan.cavecrawlers.objects;

import lombok.Data;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
public class ConfigMessage implements ConfigurationSerializable, Cloneable {
    public static boolean usePlaceholderAPI = false;
    private String message = "";
    private String title = "";
    private String subtitle = "";
    private String sound = "";
    private int duration = 100;

    public ConfigMessage(String message, String title, String subtitle, String sound, int duration) {
        this.message = message;
        this.title = title;
        this.subtitle = subtitle;
        this.sound = sound;
        this.duration = duration;
    }

    public ConfigMessage(String message, String title, String sound) {
        this.message = message;
        this.title = title;
        this.sound = sound;
    }

    public ConfigMessage(String message) {
        this.message = message;
    }

    private void sendMessageInternal(Player player){
        if (!Objects.equals(message, ""))
            player.sendMessage(message);
        if (!Objects.equals(title, "") || !Objects.equals(subtitle, ""))
            player.sendTitle(title, subtitle, 0, duration, 0);
        if (!Objects.equals(sound, ""))
            player.playSound(player.getLocation(), sound, 1, 1);
    }


    public void sendMessage(Player player) {
        sendMessage(player, new HashMap<>());
    }

    public void sendMessage(Player player, Map<String, String> placeholders){
        this.applyPlaceholders(player, placeholders).sendMessageInternal(player);
    }

    public ConfigMessage applyPlaceholders(Player player, Map<String, String> placeholders){
        ConfigMessage copy = this.clone();
        for (String key : placeholders.keySet()) {
            key = key.replaceAll("%", "");
            copy.message = copy.message.replace("%" + key + "%", placeholders.get(key));
            copy.title = copy.title.replace("%" + key + "%", placeholders.get(key));
            copy.subtitle = copy.subtitle.replace("%" + key + "%", placeholders.get(key));
        }
        if (usePlaceholderAPI){
            copy.message = PlaceholderAPI.setPlaceholders(player, copy.message);
        }
        return copy;
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("message", message);
        map.put("title", title);
        map.put("subtitle", subtitle);
        map.put("sound", sound);
        map.put("duration", duration);
        return map;
    }

    public static ConfigMessage deserialize(Map<String, Object> map){
        String message = ChatColor.translateAlternateColorCodes('&', map.get("message").toString());
        return new ConfigMessage(message, map.get("title").toString(), map.get("subtitle").toString(), map.get("sound").toString(), Integer.parseInt(map.get("duration").toString()));
    }

    @Override
    public ConfigMessage clone() {
        try {
            return (ConfigMessage) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
