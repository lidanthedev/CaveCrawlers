package me.lidan.cavecrawlers.objects;

import lombok.Data;
import org.bukkit.Sound;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Data
public class SoundOptions implements ConfigurationSerializable {
    private Sound sound;
    private float volume;
    private float pitch;

    public SoundOptions(Sound sound, float volume, float pitch) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    public SoundOptions(String sound, float volume, float pitch) {
        this(Sound.valueOf(sound), volume, pitch);
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        return Map.of(
                "sound", sound.name(),
                "volume", volume,
                "pitch", pitch
        );
    }

    public static SoundOptions deserialize(Map<String, Object> map) {
        Double volume = (Double) map.get("volume");
        Double pitch = (Double) map.get("pitch");
        String sound = map.get("sound").toString();
        return new SoundOptions(Sound.valueOf(sound), volume.floatValue(), pitch.floatValue());
    }
}
