package me.lidan.cavecrawlers.commands;

import me.lidan.cavecrawlers.gui.SellMenu;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.AutoComplete;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.annotation.CommandPermission;

public class SellCommand {
    @Command("sell")
    public void sell(Player sender) {
        SellMenu sellMenu = new SellMenu(sender);
        sellMenu.open();
    }

    @Command({"setprice","setsell"})
    @CommandPermission("cavecrawlers.sell.setprice")
    @AutoComplete("@itemID *")
    public void setPrice(Player sender, String item, double price) {
        SellMenu.config.set("prices." + item, price);
        SellMenu.config.save();
        sender.sendMessage("Price for " + item + " set to " + price);
    }
}
