package me.lidan.cavecrawlers.commands.completions;

import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.abilities.AbilityManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.node.ExecutionContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public enum AbilityIDCompletions implements SuggestionProvider<BukkitCommandActor> {
    INSTANCE;

    @Override
    public @NotNull Collection<String> getSuggestions(@NotNull ExecutionContext<BukkitCommandActor> context) {
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
}
