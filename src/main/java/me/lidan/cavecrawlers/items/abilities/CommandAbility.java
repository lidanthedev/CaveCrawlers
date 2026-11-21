package me.lidan.cavecrawlers.items.abilities;

import me.clip.placeholderapi.PlaceholderAPI;
import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;

/**
 * Runs a command on click, e.g. {@code COMMAND{command:"spawn"}} or
 * {@code COMMAND{command:"give %player% diamond",console:true}}.
 * Supports %player% and PlaceholderAPI placeholders.
 */
public class CommandAbility extends ClickAbility {
    @Setting
    private String command;
    @Setting
    private boolean console;

    public CommandAbility() {
        super("Command", "Runs a command", 0, 0);
        this.command = "";
        this.console = false;
    }

    @Override
    protected boolean useAbility(PlayerEvent playerEvent) {
        Player player = playerEvent.getPlayer();
        String resolved = command.replace("%player%", player.getName());
        if (CaveCrawlers.usePlaceholderAPI) {
            resolved = PlaceholderAPI.setPlaceholders(player, resolved);
        }
        resolved = resolved.strip();
        if (resolved.startsWith("/")) {
            resolved = resolved.substring(1);
        }
        if (resolved.isEmpty()) {
            return false;
        }
        // false (unknown command) skips mana and cooldown
        return Bukkit.dispatchCommand(console ? Bukkit.getConsoleSender() : player, resolved);
    }
}
