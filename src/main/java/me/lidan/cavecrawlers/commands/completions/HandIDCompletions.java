package me.lidan.cavecrawlers.commands.completions;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.node.ExecutionContext;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

public enum HandIDCompletions implements SuggestionProvider<BukkitCommandActor> {
    INSTANCE;

    @NotNull
    public static Set<String> getFillID(Player player) {
        ItemStack hand = player.getEquipment().getItemInMainHand();
        ItemMeta meta = hand.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return Collections.singleton("");
        }
        String name = meta.getDisplayName();
        name = ChatColor.stripColor(name);
        name = name.toUpperCase(Locale.ROOT);
        name = name.replaceAll(" ", "_");
        return Collections.singleton(name);
    }

    @Override
    public @NotNull Collection<String> getSuggestions(@NotNull ExecutionContext<BukkitCommandActor> context) {
        Player player = context.actor().asPlayer();
        if (player == null) {
            return Collections.singleton("");
        }
        return getFillID(player);
    }
}
