package me.lidan.cavecrawlers.commands.completions;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.lidan.cavecrawlers.CaveCrawlers;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.node.ExecutionContext;

import java.util.Collection;
import java.util.Collections;

public enum SkillIDCompletions implements SuggestionProvider<BukkitCommandActor> {
    INSTANCE;

    @Override
    public @NotNull Collection<String> getSuggestions(@NotNull ExecutionContext<BukkitCommandActor> context) {
        MythicBukkit mythicBukkit = CaveCrawlers.getInstance().getMythicBukkit();
        if (mythicBukkit == null) {
            return Collections.emptySet();
        }
        return mythicBukkit.getSkillManager().getSkillNames();
    }
}
