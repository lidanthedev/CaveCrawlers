package me.lidan.cavecrawlers.items.abilities;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an ability field as settable from the JSON settings in an ability ID,
 * e.g. {@code BASIC_LASER{cost:5,particle:FLAME}}. Bound by {@link AbilitySettings}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Setting {
    /** JSON key; defaults to the field name. */
    String value() default "";
}
