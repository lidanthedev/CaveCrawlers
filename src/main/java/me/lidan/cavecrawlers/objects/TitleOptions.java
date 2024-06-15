package me.lidan.cavecrawlers.objects;

import lombok.Data;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Data
public class TitleOptions implements ConfigurationSerializable {
    private String title;
    private String subtitle;
    private int fadeIn;
    private int stay;
    private int fadeOut;

    public TitleOptions(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        this.title = ChatColor.translateAlternateColorCodes('&',title);
        this.subtitle = ChatColor.translateAlternateColorCodes('&',subtitle);
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
    }

    public TitleOptions replace(@NotNull CharSequence target, @NotNull CharSequence replacement) {
        return new TitleOptions(title.replace(target, replacement), subtitle.replace(target, replacement), fadeIn, stay, fadeOut);
    }

    public TitleOptions setPlaceholders(Player player){
        return new TitleOptions(PlaceholderAPI.setPlaceholders(player, title),
                PlaceholderAPI.setPlaceholders(player, subtitle), fadeIn, stay, fadeOut);
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        return Map.of(
                "title", title,
                "subtitle", subtitle,
                "fadeIn", fadeIn,
                "stay", stay,
                "fadeOut", fadeOut
        );
    }

    public static TitleOptions deserialize(Map<String, Object> map) {
        return new TitleOptions((String) map.get("title"), (String) map.get("subtitle"), (int) map.get("fadeIn"), (int) map.get("stay"), (int) map.get("fadeOut"));
    }
}
