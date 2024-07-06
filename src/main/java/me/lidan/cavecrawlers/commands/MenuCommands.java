package me.lidan.cavecrawlers.commands;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.gui.MenuGui;
import me.lidan.cavecrawlers.gui.PlayerViewer;
import me.lidan.cavecrawlers.gui.SimpleMenuGui;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Optional;

public class MenuCommands {
    public static final String MENU_TYPE_SIMPLE = "simple";
    public static final String MENU_TYPE_ADVANCED = "advanced";
    private static final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private static final String MENU_TYPE = plugin.getConfig().getString("menu-type", MENU_TYPE_SIMPLE);
    private static final Logger log = LoggerFactory.getLogger(MenuCommands.class);

    @Command("menu")
    public void menu(Player sender) {
        if(MENU_TYPE.equalsIgnoreCase(MENU_TYPE_SIMPLE)) {
            new SimpleMenuGui(sender).open();
        }
        else if (MENU_TYPE.equalsIgnoreCase(MENU_TYPE_ADVANCED)){
            new MenuGui(sender).open();
        }
        else{
            sender.performCommand(MENU_TYPE);
        }
    }

    @Command({"playerviewer","profile","myprofile"})
    public void playerViewerOpen(Player sender, @Optional Player arg){
        if(arg == null) {
            arg = sender;
        }
        new PlayerViewer(arg).open(sender);
    }
}
