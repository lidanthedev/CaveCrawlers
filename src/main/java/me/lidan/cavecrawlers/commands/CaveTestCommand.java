package me.lidan.cavecrawlers.commands;

import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.utils.CustomConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import revxrsal.commands.CommandHandler;
import revxrsal.commands.annotation.AutoComplete;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.List;
import java.util.Objects;

@Command({"cavetest", "ct"})
@CommandPermission("cavecrawlers.test")
public class CaveTestCommand {

    private CustomConfig config = new CustomConfig("test");
    private StatsManager statsManager = StatsManager.getInstance();
    private CommandHandler handler;

    public CaveTestCommand(CommandHandler handler) {
        this.handler = handler;
        handler.getAutoCompleter().registerSuggestion("itemID", (args, sender, command) -> ItemsManager.getInstance().getKeys());
    }

    @Subcommand("config saveStats")
    public void saveStats(Player sender){
        config.set("stat", statsManager.getStats(sender));
        sender.sendMessage("set stat to your stats!");
        config.save();
    }

    @Subcommand("config send")
    public void sendConfig(Player sender, String key){
        sender.sendMessage("" + config.get(key));
    }

    @Subcommand("config save")
    public void saveConfig(CommandSender sender){
        config.save();
    }

    @Subcommand("config reload")
    public void reloadConfig(CommandSender sender){
        config.load();
    }

    @Subcommand("item test")
    public void itemTest(Player sender){
        ItemStack exampleSword = ItemsManager.getInstance().buildItem("EXAMPLE_SWORD");
        sender.getInventory().addItem(exampleSword);
    }

    @Subcommand("item getID")
    public void itemGetID(Player sender){
        ItemStack hand = sender.getInventory().getItemInMainHand();
        String ID = ItemsManager.getInstance().getIDofItemStack(hand);
        sender.sendMessage(Objects.requireNonNullElse(ID, "This is not a Custom Item!"));
    }

    @Subcommand("item update")
    public void itemUpdate(Player sender){
        ItemStack hand = sender.getInventory().getItemInMainHand();
        String ID = ItemsManager.getInstance().getIDofItemStack(hand);
        if (ID == null){
            sender.sendMessage("This is not a Custom Item!");
        }
        else{
            ItemStack updatedItem = ItemsManager.getInstance().buildItem(ID);
            sender.getInventory().setItemInMainHand(updatedItem);
            sender.sendMessage("Updated Item with ID " + ID);
        }
    }

    @Subcommand("item give")
    @AutoComplete("@itemID *")
    public void itemGive(Player sender, String ID){
        ItemStack exampleSword = ItemsManager.getInstance().buildItem(ID);
        sender.getInventory().addItem(exampleSword);
    }

}
