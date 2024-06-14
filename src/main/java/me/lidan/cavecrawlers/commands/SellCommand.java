package me.lidan.cavecrawlers.commands;

import me.lidan.cavecrawlers.gui.SellMenu;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
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
    public void setPrice(Player sender, String itemId, double price) {
        ItemInfo itemInfo = ItemsManager.getInstance().getItemByID(itemId);
        if (itemInfo == null){
            sender.sendMessage("ERROR! ITEM DOESN'T EXIST!");
            return;
        }
        if (itemId.contains(".")) return;
        SellMenu.config.set("prices." + itemId, price);
        SellMenu.config.save();
        sender.sendMessage("Price for " + itemId + " set to " + price);
    }
}
