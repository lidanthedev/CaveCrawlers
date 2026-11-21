package me.lidan.cavecrawlers.commands.completions;

import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.abilities.AbilityManager;
import me.lidan.cavecrawlers.items.abilities.AbilitySettings;
import me.lidan.cavecrawlers.items.abilities.ItemAbility;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.node.ExecutionContext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public enum AbilityIDCompletions implements SuggestionProvider<BukkitCommandActor> {
    INSTANCE;

    @Override
    public @NotNull Collection<String> getSuggestions(@NotNull ExecutionContext<BukkitCommandActor> context) {
        String input = context.input().source();
        int brace = input.indexOf('{');
        if (brace >= 0) {
            // ability IDs have no spaces, so the token starts after the last space before the brace
            String token = input.substring(input.lastIndexOf(' ', brace) + 1);
            ItemAbility ability = AbilityManager.getInstance().getAbilityMap().get(token.substring(0, token.indexOf('{')));
            return ability == null ? List.of() : suggestSettings(token, ability);
        }

        Player player = context.actor().asPlayer();
        ItemInfo itemInfo = player == null ? null : ItemsManager.getInstance().getItemFromItemStack(player.getEquipment().getItemInMainHand());
        String heldAbilityID = itemInfo == null ? null : itemInfo.getAbilityID();

        List<String> suggestions = new ArrayList<>(AbilityManager.getInstance().getAbilityMap().keySet().stream()
                .filter(s -> !s.contains("{"))
                .toList());
        // held ID added directly: settings IDs only enter abilityMap after first lookup
        if (heldAbilityID != null && !suggestions.contains(heldAbilityID)) {
            suggestions.add(heldAbilityID);
        }
        return suggestions;
    }

    /**
     * Suggests full replacements for a partially typed settings token such as BASIC_LASER{cost:5,par -
     * unused keys after '{' or ',', and known values after "key:".
     * Only top-level keys are completed; nested arrays/objects get nothing.
     */
    static List<String> suggestSettings(String token, ItemAbility ability) {
        Map<String, Field> fields = AbilitySettings.fields(ability.getClass());
        Set<String> usedKeys = new HashSet<>();
        int depth = 0;
        int segmentStart = -1;
        char quote = 0;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (quote != 0) {
                if (c == '\\') i++;
                else if (c == quote) quote = 0;
                continue;
            }
            switch (c) {
                case '"', '\'' -> quote = c;
                case '{', '[' -> {
                    if (++depth == 1) segmentStart = i + 1;
                }
                case '}', ']' -> depth--;
                case ',' -> {
                    if (depth == 1) {
                        usedKeys.add(keyOf(token.substring(segmentStart, i)));
                        segmentStart = i + 1;
                    }
                }
                default -> {
                }
            }
        }
        if (depth != 1 || quote != 0) {
            return List.of();
        }

        String head = token.substring(0, segmentStart);
        String segment = token.substring(segmentStart);
        int separator = separatorIndex(segment);
        List<String> suggestions = new ArrayList<>();
        if (separator < 0) {
            // keep typed whitespace (e.g. "{cost:5, ") so suggestions still match the token
            String keyPrefix = head + segment.substring(0, segment.length() - segment.stripLeading().length());
            for (String key : fields.keySet()) {
                if (!usedKeys.contains(key)) suggestions.add(keyPrefix + key + ":");
            }
        } else {
            Field field = fields.get(keyOf(segment));
            if (field != null && !field.getType().isArray()) {
                String afterSeparator = segment.substring(separator + 1);
                String keyPart = segment.substring(0, segment.length() - afterSeparator.stripLeading().length());
                for (String value : AbilitySettings.values(field.getType())) {
                    suggestions.add(head + keyPart + value);
                }
            }
        }
        // brigadier passes suggestions through unfiltered, so filter by what was typed
        suggestions.removeIf(s -> !s.regionMatches(true, 0, token, 0, token.length()));
        return suggestions;
    }

    private static int separatorIndex(String segment) {
        int colon = segment.indexOf(':');
        int equals = segment.indexOf('=');
        if (colon < 0) return equals;
        if (equals < 0) return colon;
        return Math.min(colon, equals);
    }

    private static String keyOf(String segment) {
        int separator = separatorIndex(segment);
        String key = (separator < 0 ? segment : segment.substring(0, separator)).trim();
        if (key.length() >= 2 && (key.charAt(0) == '"' || key.charAt(0) == '\'')) {
            key = key.substring(1, key.length() - 1);
        }
        return key;
    }
}
