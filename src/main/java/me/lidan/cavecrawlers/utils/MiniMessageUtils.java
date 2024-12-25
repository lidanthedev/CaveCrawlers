package me.lidan.cavecrawlers.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Map;

public class MiniMessageUtils {
    public static Component miniMessageString(String message) {
        return MiniMessage.miniMessage().deserialize(message);
    }

    public static Component miniMessageString(String message, Map<String, String> placeholders) {
        TagResolver[] resolvers = placeholders.entrySet().stream()
                .map(entry -> Placeholder.parsed(entry.getKey(), entry.getValue()))
                .toArray(TagResolver[]::new);

        return MiniMessage.miniMessage().deserialize(message, resolvers);
    }

    public static Component miniMessageComponent(String message, Map<String, Component> placeholders) {
        TagResolver[] resolvers = placeholders.entrySet().stream()
                .map(entry -> Placeholder.component(entry.getKey(), entry.getValue()))
                .toArray(TagResolver[]::new);

        return MiniMessage.miniMessage().deserialize(message, resolvers);
    }

    public static String miniMessageFromStringToString(String message, Map<String, String> placeholders) {
        return componentToString(miniMessageString(message, placeholders));
    }

    public static String componentToString(Component message) {
        return PlainTextComponentSerializer.plainText().serialize(message);
    }

}
