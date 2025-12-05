package me.lidan.cavecrawlers.commands;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.gui.MenuGui;
import me.lidan.cavecrawlers.gui.PlayerViewer;
import me.lidan.cavecrawlers.gui.SimpleMenuGui;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.bukkit.annotation.CommandPermission;

public class MenuCommands {
    public static final String MENU_TYPE_SIMPLE = "simple";
    public static final String MENU_TYPE_ADVANCED = "advanced";
    private static final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private static final String MENU_TYPE = plugin.getConfig().getString("menu.type", MENU_TYPE_SIMPLE);
    private static final String MENU_COMMAND = plugin.getConfig().getString("menu.command", "openmenu");

    public static void showMenu(Player sender) {
        if(MENU_TYPE.equalsIgnoreCase(MENU_TYPE_SIMPLE)) {
            new SimpleMenuGui(sender).open();
        }
        else if (MENU_TYPE.equalsIgnoreCase(MENU_TYPE_ADVANCED)){
            new MenuGui(sender).open();
        }
        else{
            sender.performCommand(MENU_COMMAND);
        }
    }

    @Command("ctmenu")
    public void menu(Player sender) {
        showMenu(sender);
    }

    @Command({"playerviewer","profile","myprofile"})
    @CommandPermission("cavecrawlers.playerviewer")
    public void playerViewerOpen(Player sender, @Optional Player arg){
        if(arg == null) {
            arg = sender;
        }
        if (!sender.hasPermission("cavecrawlers.playerviewer.others") && arg != sender) {
            sender.sendMessage(MiniMessageUtils.miniMessageString("<red>You don't have permissions to view other profiles</red>"));
            return;
        }
        new PlayerViewer(arg).open(sender);
    }
}
