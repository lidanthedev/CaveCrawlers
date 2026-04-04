package me.lidan.cavecrawlers.objects;

import lombok.Data;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
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

    @SuppressWarnings("unused")
    public SoundOptions(String sound, float volume, float pitch) {
        this(resolveSound(sound), volume, pitch);
    }

    public static SoundOptions deserialize(Map<String, Object> map) {
        Number volume = (Number) map.get("volume");
        Number pitch = (Number) map.get("pitch");
        String sound = map.get("sound").toString();
        return new SoundOptions(resolveSound(sound), volume.floatValue(), pitch.floatValue());
    }

    private static Sound resolveSound(String sound) {
        NamespacedKey key = NamespacedKey.fromString(sound);
        if (key == null) {
            key = NamespacedKey.minecraft(sound.toLowerCase());
        }

        Sound resolved = Registry.SOUNDS.get(key);
        if (resolved == null) {
            throw new IllegalArgumentException("Unknown sound: " + sound);
        }
        return resolved;
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        NamespacedKey soundKey = Registry.SOUNDS.getKey(sound);
        if (soundKey == null) {
            throw new IllegalStateException("Cannot serialize unregistered sound: " + sound);
        }

        return Map.of(
                "sound", soundKey.toString(),
                "volume", volume,
                "pitch", pitch
        );
    }
}
