package me.lidan.cavecrawlers.objects;

import lombok.Data;
import me.clip.placeholderapi.PlaceholderAPI;
import me.lidan.cavecrawlers.levels.LevelInfo;
import me.lidan.cavecrawlers.stats.ActionBarManager;
import me.lidan.cavecrawlers.utils.CustomConfig;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Data
public class ConfigMessage implements ConfigurationSerializable, Cloneable {
    public static boolean usePlaceholderAPI = false;
    private static CustomConfig config = new CustomConfig("messages");
    private String message = "";
    private TitleOptions titleOptions;
    private String actionbar = "";
    private SoundOptions sound;
    private LevelInfo levelInfo;

    public ConfigMessage(String message, TitleOptions titleOptions, String actionbar, SoundOptions sound) {
        this.message = ChatColor.translateAlternateColorCodes('&', message);
        this.actionbar = ChatColor.translateAlternateColorCodes('&', actionbar);
        this.titleOptions = titleOptions;
        this.sound = sound;
    }

    public ConfigMessage(String message, String title, String subtitle, SoundOptions sound, int duration, String actionbar) {
        this(message, new TitleOptions(title, subtitle, 0, duration, 0), actionbar, sound);
    }

    public ConfigMessage(String message, String title, SoundOptions sound) {
        this(message, title, "", sound, 20, "");
    }

    public ConfigMessage(String message, String title, Sound sound) {
        this(message, title, new SoundOptions(sound, 1, 1));
    }

    public ConfigMessage(String message) {
        this(message, "", (SoundOptions) null);
    }

    private void sendMessageInternal(Player player){
        if (message != null && !message.isEmpty())
            player.sendMessage(message);
        if (titleOptions != null)
            player.sendTitle(titleOptions.getTitle(), titleOptions.getSubtitle(), titleOptions.getFadeIn(), titleOptions.getStay(), titleOptions.getFadeOut());
        if (actionbar != null && !actionbar.isEmpty())
            ActionBarManager.getInstance().showActionBar(player, actionbar);
        if (sound != null)
            player.playSound(player.getLocation(), sound.getSound(), sound.getVolume(), sound.getPitch());
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
            copy.actionbar = copy.actionbar.replace("%" + key + "%", placeholders.get(key));
            if(copy.titleOptions != null)
                copy.titleOptions = copy.titleOptions.replace("%" + key + "%", placeholders.get(key));
        }
        if (usePlaceholderAPI){
            copy.message = PlaceholderAPI.setPlaceholders(player, copy.message);
            if(copy.titleOptions != null)
                copy.titleOptions = copy.titleOptions.setPlaceholders(player);
            copy.actionbar = PlaceholderAPI.setPlaceholders(player, copy.actionbar);
        }
        return copy;
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("message", message);
        map.put("title", titleOptions);
        map.put("actionbar", actionbar);
        map.put("sound", sound);
        return map;
    }

    public static ConfigMessage deserialize(Map<String, Object> map){
        String message = ChatColor.translateAlternateColorCodes('&', map.get("message").toString());
        TitleOptions titleOptions = map.containsKey("title") ? (TitleOptions) map.get("title") : null;
        String actionbar = map.containsKey("actionbar") ? map.get("actionbar").toString() : "";
        SoundOptions sound = map.containsKey("sound") ? (SoundOptions) map.get("sound") : null;
        actionbar = ChatColor.translateAlternateColorCodes('&', actionbar);
        return new ConfigMessage(message, titleOptions, actionbar, sound);
    }

    @Override
    public ConfigMessage clone() {
        try {
            return (ConfigMessage) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public static @Nullable ConfigMessage getMessage(String key){
        if (key == null){
            return null;
        }
        return config.getSerializable(key, ConfigMessage.class);
    }

    public static @Nullable String getIdOfMessage(ConfigMessage message) {
        for (String key : config.getKeys(false)) {
            ConfigMessage configMessage = getMessage(key);
            if (configMessage != null && configMessage.equals(message)){
                return key;
            }
        }
        return null;
    }

    public static ConfigMessage getMessageOrDefault(String key, ConfigMessage defaultMessage){
        return config.getSerializable(key, ConfigMessage.class, defaultMessage);
    }

    public static ConfigMessage getMessageOrDefault(String key, String defaultMessage){
        return getMessageOrDefault(key, new ConfigMessage(defaultMessage));
    }
}
