package me.lidan.cavecrawlers.commands.completions;

import me.lidan.cavecrawlers.items.abilities.AbilityManager;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.node.ExecutionContext;

import java.util.Collection;

public enum AbilityIDCompletions implements SuggestionProvider<BukkitCommandActor> {
    INSTANCE;

    @Override
    public @NotNull Collection<String> getSuggestions(@NotNull ExecutionContext<BukkitCommandActor> context) {
        return AbilityManager.getInstance().getAbilityMap().keySet();
    }
}
