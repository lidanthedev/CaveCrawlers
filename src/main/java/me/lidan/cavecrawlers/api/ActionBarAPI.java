package me.lidan.cavecrawlers.api;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * API for handling action bar interactions in the CaveCrawlers plugin.
 * Implementations should provide methods for displaying and updating action bar messages.
 */
public interface ActionBarAPI {
    void actionBar(Player player, String alert);

    void actionBar(Player player, Component alert);

    void actionBar(Player player);
}
