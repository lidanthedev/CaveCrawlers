package me.lidan.cavecrawlers.stats;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Identifies player absorption without assuming Mojang or obfuscated field names. */
final class AbsorptionAccessorResolver {
    private AbsorptionAccessorResolver() {
    }

    static Object resolve(Class<?> playerType, Class<?> livingType, Class<?> accessorType,
                          Predicate<Object> isFloatAccessor) {
        if (!livingType.isAssignableFrom(playerType) || playerType == livingType) {
            throw new IllegalStateException("Cannot identify the player metadata hierarchy");
        }
        List<Object> candidates = new ArrayList<>();
        Object named = null;
        // Never include LivingEntity's float HEALTH accessor in the fallback candidates.
        for (Class<?> type = playerType; type != livingType; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || !accessorType.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(null);
                    if (value != null && isFloatAccessor.test(value)) {
                        candidates.add(value);
                        if (field.getName().equals("DATA_PLAYER_ABSORPTION_ID")) {
                            if (named != null && named != value) {
                                throw new IllegalStateException("Ambiguous named absorption metadata accessors");
                            }
                            named = value;
                        }
                    }
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Cannot inspect player metadata accessors", exception);
                }
            }
        }
        if (named != null) {
            return named;
        }
        if (candidates.size() == 1) {
            return candidates.getFirst();
        }
        throw new IllegalStateException("Expected one player-specific float metadata accessor, found " + candidates.size());
    }
}
