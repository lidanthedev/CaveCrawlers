package me.lidan.cavecrawlers.commands.completions;

import me.lidan.cavecrawlers.shop.ShopManager;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.node.ExecutionContext;

import java.util.Collection;

public enum ShopIDCompletions implements SuggestionProvider<BukkitCommandActor> {
    INSTANCE;

    @Override
    public @NotNull Collection<String> getSuggestions(@NotNull ExecutionContext<BukkitCommandActor> context) {
        return ShopManager.getInstance().getKeys();
    }
}
