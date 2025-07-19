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
    void showActionBar(Player player, String alert);

    /**
     * Sends an action bar message to the player using a Component alert.
     *
     * @param player the player to send the action bar message to
     * @param alert  the Component message to display
     */
    void showActionBar(Player player, Component alert);

    /**
     * Displays the default action bar.
     *
     * @param player the player to send the action bar message to
     */
    void showActionBar(Player player);
}
