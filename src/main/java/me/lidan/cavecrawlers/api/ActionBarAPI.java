package me.lidan.cavecrawlers.api;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * API for handling action bar interactions in the CaveCrawlers plugin.
 * Implementations should provide methods for displaying and updating action bar messages.
 */
public interface ActionBarAPI {
    /**
     * Sends an action bar message to the player using a String alert.
     *
     * @param player the player to send the action bar message to
     * @param alert  the message to display
     */
    void actionBar(Player player, String alert);

    /**
     * Sends an action bar message to the player using a Component alert.
     *
     * @param player the player to send the action bar message to
     * @param alert  the Component message to display
     */
    void actionBar(Player player, Component alert);

    /**
     * Clears the action bar for the player or displays a default message.
     *
     * @param player the player whose action bar will be affected
     */
    void actionBar(Player player);
}
