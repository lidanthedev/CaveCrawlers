package me.lidan.cavecrawlers.commands;

import me.lidan.cavecrawlers.gui.MenuGui;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;

public class MenuCommand {
    @Command("menu")
    public void menu(Player sender) {
        MenuGui gui = new MenuGui(sender);
        gui.open();
    }
}
