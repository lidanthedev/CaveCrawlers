package me.lidan.cavecrawlers.commands;

import me.lidan.cavecrawlers.gui.MenuGui;
import me.lidan.cavecrawlers.gui.PlayerViewer;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Optional;

public class MenuCommands {
    @Command("menu")
    public void menu(Player sender) {
        MenuGui gui = new MenuGui(sender);
        gui.open();
    }

    @Command({"playerviewer","profile","myprofile"})
    public void playerViewerOpen(Player sender, @Optional Player arg){
        if(arg == null) {
            arg = sender;
        }
        new PlayerViewer(arg).open(sender);
    }
}
