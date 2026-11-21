package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.lidan.cavecrawlers.objects.SoundOptions;
import me.lidan.cavecrawlers.stats.StatType;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Binds JSON ability settings onto {@link Setting} fields.
 * Conversions use the same Gson getters / valueOf calls the hand-written parsers used,
 * so existing settings strings keep their exact meaning.
 */
public final class AbilitySettings {
    private static final Map<Class<?>, Map<String, Field>> FIELDS = new ConcurrentHashMap<>();

    private AbilitySettings() {
    }

    /** Settable keys for an ability class (superclass keys first), mapped to their fields. */
    public static Map<String, Field> fields(Class<? extends ItemAbility> type) {
        // not computeIfAbsent: scan recurses into superclasses, and ConcurrentHashMap forbids recursive updates
        Map<String, Field> cached = FIELDS.get(type);
        if (cached == null) {
            cached = scan(type);
            FIELDS.putIfAbsent(type, cached);
        }
        return cached;
    }

    // Unknown keys are ignored, as before: old item configs may carry keys an ability never read (e.g. Hulk "material").
    static void bind(ItemAbility ability, JsonObject settings) {
        Map<String, Field> fields = fields(ability.getClass());
        for (Map.Entry<String, JsonElement> entry : settings.entrySet()) {
            Field field = fields.get(entry.getKey());
            if (field == null) {
                continue;
            }
            try {
                field.set(ability, convert(field.getType(), entry.getValue()));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot set ability setting " + entry.getKey(), e);
            }
        }
    }

    /** Values worth suggesting for a setting of this type; empty when free-form. */
    public static List<String> values(Class<?> type) {
        Class<?> target = type.isArray() ? type.getComponentType() : type;
        if (target == boolean.class || target == Boolean.class) {
            return List.of("true", "false");
        }
        if (target == StatType.class) {
            return StatType.names();
        }
        if (target.isEnum()) {
            return Arrays.stream(target.getEnumConstants()).map(c -> ((Enum<?>) c).name()).toList();
        }
        return Collections.emptyList();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convert(Class<?> type, JsonElement value) {
        if (type == String.class) return value.getAsString();
        if (type == double.class || type == Double.class) return value.getAsDouble();
        if (type == long.class || type == Long.class) return value.getAsLong();
        if (type == int.class || type == Integer.class) return value.getAsInt();
        if (type == float.class || type == Float.class) return value.getAsFloat();
        if (type == boolean.class || type == Boolean.class) return value.getAsBoolean();
        if (type == StatType.class) return StatType.valueOf(value.getAsString());
        if (type == Sound.class) return SoundOptions.resolveSound(value.getAsString());
        // getByName returns null for unknown names; kept to match the old parser
        if (type == PotionEffectType.class) return PotionEffectType.getByName(value.getAsString());
        if (type.isEnum()) return Enum.valueOf((Class) type, value.getAsString());
        if (type.isArray()) {
            JsonArray array = value.getAsJsonArray();
            Object result = Array.newInstance(type.getComponentType(), array.size());
            for (int i = 0; i < array.size(); i++) {
                Array.set(result, i, convert(type.getComponentType(), array.get(i)));
            }
            return result;
        }
        throw new IllegalArgumentException("Unsupported ability setting type: " + type.getName());
    }

    private static Map<String, Field> scan(Class<?> type) {
        Map<String, Field> fields = new LinkedHashMap<>();
        if (type.getSuperclass() != null && ItemAbility.class.isAssignableFrom(type.getSuperclass())) {
            fields.putAll(fields(type.getSuperclass().asSubclass(ItemAbility.class)));
        }
        for (Field field : type.getDeclaredFields()) {
            Setting setting = field.getAnnotation(Setting.class);
            if (setting == null) {
                continue;
            }
            field.setAccessible(true);
            fields.put(setting.value().isEmpty() ? field.getName() : setting.value(), field);
        }
        return Collections.unmodifiableMap(fields);
    }
}
