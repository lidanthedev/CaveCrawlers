package me.lidan.cavecrawlers.objects;

import lombok.Data;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
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

    public static SoundOptions deserialize(Map<String, Object> map) {
        Number volume = (Number) map.get("volume");
        Number pitch = (Number) map.get("pitch");
        String sound = map.get("sound").toString();
        return new SoundOptions(resolveSound(sound), volume.floatValue(), pitch.floatValue());
    }

    public static Sound resolveSound(String sound) {
        NamespacedKey key = NamespacedKey.fromString(sound);
        if (key != null) {
            Sound resolved = Registry.SOUNDS.get(key);
            if (resolved != null) {
                return resolved;
            }
        }

        Sound resolvedMinecraft = Registry.SOUNDS.get(NamespacedKey.minecraft(sound.toLowerCase(Locale.ROOT)));
        if (resolvedMinecraft != null) {
            return resolvedMinecraft;
        }

        // Backwards compatibility: support legacy enum-style values like ENTITY_WITHER_SPAWN.
        String normalizedInput = normalizeSoundId(sound);
        for (Sound candidate : Registry.SOUNDS) {
            NamespacedKey candidateKey = Registry.SOUNDS.getKey(candidate);
            if (candidateKey == null) {
                continue;
            }

            String normalizedPath = normalizeSoundId(candidateKey.getKey());
            if (normalizedInput.equals(normalizedPath)) {
                return candidate;
            }

            String normalizedNamespaced = normalizeSoundId(candidateKey.toString());
            if (normalizedInput.equals(normalizedNamespaced)) {
                return candidate;
            }
        }

        throw new IllegalArgumentException("Unknown sound: " + sound);
    }

    private static String normalizeSoundId(String value) {
        StringBuilder normalized = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                normalized.append(Character.toLowerCase(c));
            }
        }
        return normalized.toString();
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
