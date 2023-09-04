package me.lidan.cavecrawlers.commands;

import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.utils.CustomConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;

@Command({"cavetest", "ct"})
public class CaveTestCommand {

    private CustomConfig config = new CustomConfig("test");
    private StatsManager statsManager = StatsManager.getInstance();

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


}
