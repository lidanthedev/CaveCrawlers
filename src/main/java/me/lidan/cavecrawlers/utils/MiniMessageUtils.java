package me.lidan.cavecrawlers.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * MiniMessageUtils class to manage MiniMessage
 * Making it easier to use MiniMessage
 */
public class MiniMessageUtils {

    public static final @NotNull MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    public static final @NotNull LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.builder().hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    /**
     * Convert a string to a MiniMessage Component
     *
     * @param message the message in MiniMessage format
     * @return the MiniMessage Component
     */
    public static Component miniMessage(String message) {
        return miniMessage(message, Map.of());
    }

    /**
     * Convert a string to a MiniMessage Component with placeholders accepts any object
     * This method is created to fit with the old method signature
     *
     * @param message      the message in MiniMessage format
     * @param placeholders the placeholders as objects
     * @return the MiniMessage Component
     */
    public static Component miniMessageAuto(String message, Map<String, Object> placeholders) {
        return miniMessage(message, placeholders);
    }

    /**
     * Convert a string to a MiniMessage Component with placeholders accepts any object
     *
     * @param message      the message in MiniMessage format
     * @param placeholders the placeholders as objects
     * @return the MiniMessage Component
     */
    public static Component miniMessage(String message, Map<String, Object> placeholders) {
        if (placeholders.isEmpty()) {
            return miniMessageString(message);
        }
        TagResolver[] resolvers = placeholders.entrySet().stream()
                .map(entry -> {
                    if (entry.getValue() instanceof Component) {
                        return Placeholder.component(entry.getKey(), (Component) entry.getValue());
                    } else if (entry.getValue() instanceof String str && str.contains(String.valueOf(ChatColor.COLOR_CHAR))) {
                        return Placeholder.component(entry.getKey(), LEGACY_SECTION.deserialize(str));
                    } else {
                        return Placeholder.parsed(entry.getKey(), entry.getValue().toString());
                    }
                })
                .toArray(TagResolver[]::new);

        return MINI_MESSAGE.deserialize(message, resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /**
     * Convert a string to a MiniMessage Component
     *
     * @param message the message in MiniMessage format
     * @return the MiniMessage Component
     */
    public static Component miniMessageString(String message) {
        return MINI_MESSAGE.deserialize(message).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /**
     * Convert a string to a MiniMessage Component with placeholders
     * @param message the message in MiniMessage format
     * @param placeholders the placeholders as strings
     * @return the MiniMessage Component
     */
    public static Component miniMessageString(String message, Map<String, String> placeholders) {
        TagResolver[] resolvers = placeholders.entrySet().stream()
                .map(entry -> Placeholder.parsed(entry.getKey(), entry.getValue()))
                .toArray(TagResolver[]::new);

        return MINI_MESSAGE.deserialize(message, resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /**
     * Convert a string to a MiniMessage Component with placeholders
     * @param message the message in MiniMessage format
     * @param placeholders the placeholders as components
     * @return the MiniMessage Component
     */
    public static Component miniMessageComponent(String message, Map<String, Component> placeholders) {
        TagResolver[] resolvers = placeholders.entrySet().stream()
                .map(entry -> Placeholder.component(entry.getKey(), entry.getValue()))
                .toArray(TagResolver[]::new);
        return MINI_MESSAGE.deserialize(message, resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /**
     * Convert a MiniMessage Component to a string
     * @param message the MiniMessage Component
     * @param placeholders the placeholders as strings
     * @return the message as a string
     */
    public static String miniMessageFromStringToString(String message, Map<String, String> placeholders) {
        return componentToString(miniMessageString(message, placeholders));
    }

    /**
     * Convert a MiniMessage Component to a string
     * @param message the MiniMessage Component
     * @return the message as a string
     */
    public static String componentToString(Component message) {
        return PlainTextComponentSerializer.plainText().serialize(message);
    }

    /**
     * Convert a MiniMessage Component to a legacy string
     * This method is not recommended to use
     *
     * @param message the MiniMessage Component
     * @return the message as a legacy string
     */
    public static String componentToLegacyString(Component message) {
        return LEGACY_SECTION.serialize(message);
    }

    /**
     * Create a progress bar with a specific length
     * @param current the current value
     * @param max the max value
     * @param length the length of the progress bar
     * @return the progress bar as a component
     */
    public static Component progressBar(double current, double max, int length) {
        return progressBar(current, max, length, "-");
    }

    /**
     * Create a progress bar with a specific length and icon
     * @param current the current value
     * @param max the max value
     * @param length the length of the progress bar
     * @param icon the icon to use
     * @return the progress bar as a component
     */
    public static Component progressBar(double current, double max, int length, String icon) {
        StringBuilder progressBar = new StringBuilder();
        double percent = current / max * 100d;
        for (int i = 0; i < length; i++) {
            if (i < percent / (100d / length)) {
                progressBar.append("<green>");
            } else {
                progressBar.append("<white>");
            }
            progressBar.append(icon);
        }
        return MINI_MESSAGE.deserialize(progressBar.toString());
    }
}
