package me.lidan.cavecrawlers.api;

import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

/**
 * API for handling prompts and messages in the CaveCrawlers plugin.
 * Implementations should provide methods for displaying prompts to players.
 */
public interface PromptAPI {
    /**
     * Prompts a player with a title and returns their response as a String.
     *
     * @param player      the player to prompt
     * @param promptTitle the title of the prompt
     * @return a CompletableFuture containing the player's response
     */
    CompletableFuture<String> prompt(Player player, String promptTitle);

    /**
     * Prompts a player with a title and subtitle and returns their response as a String.
     *
     * @param player the player to prompt
     * @param promptTitle the title of the prompt
     * @param promptSubtitle the subtitle of the prompt
     * @return a CompletableFuture containing the player's response
     */
    CompletableFuture<String> prompt(Player player, String promptTitle, String promptSubtitle);

    /**
     * Prompts a player for a number and returns their response as an Integer.
     *
     * @param player the player to prompt
     * @param promptTitle the title of the prompt
     * @return a CompletableFuture containing the player's numeric response
     */
    CompletableFuture<Integer> promptNumber(Player player, String promptTitle);

    /**
     * Prompts a player for a number with a minimum value and returns their response as an Integer.
     *
     * @param player the player to prompt
     * @param promptTitle the title of the prompt
     * @param min the minimum value allowed
     * @return a CompletableFuture containing the player's numeric response
     */
    CompletableFuture<Integer> promptNumberMin(Player player, String promptTitle, int min);
}
