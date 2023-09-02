package me.lidan.cavecrawlers.commands;

import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command({"stats", "stat", "statadmin"})
@CommandPermission("stats.admin")
public class StatCommand {

    @Subcommand("list")
    public void listStats(Player sender, Player arg){
        if(arg == null) {
            arg = sender;
        }
        Stats stats = StatsManager.getInstance().getStats(arg);
        sender.sendMessage(stats.toFormatString());
    }
}
