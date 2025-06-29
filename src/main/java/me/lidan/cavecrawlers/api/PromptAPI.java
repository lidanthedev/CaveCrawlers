package me.lidan.cavecrawlers.api;

import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

/**
 * API for handling prompts and messages in the CaveCrawlers plugin.
 * Implementations should provide methods for displaying prompts to players.
 */
public interface PromptAPI {
    CompletableFuture<String> prompt(Player player, String promptTitle);

    CompletableFuture<String> prompt(Player player, String promptTitle, String promptSubtitle);

    CompletableFuture<Integer> promptNumber(Player player, String promptTitle);

    CompletableFuture<Integer> promptNumberMin(Player player, String promptTitle, int min);
}
