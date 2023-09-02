package me.lidan.cavecrawlers.commands;

import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command({"stats", "stat", "statadmin"})
@CommandPermission("stats.admin")
public class StatCommand {

    @Subcommand("list")
    public void listStats(Player sender, @Optional Player arg){
        if(arg == null) {
            arg = sender;
        }
        Stats stats = StatsManager.getInstance().getStats(arg);
        sender.sendMessage(stats.toFormatString());
    }

    @Subcommand("lore")
    public void loreStats(Player sender, @Optional Player arg){
        if(arg == null) {
            arg = sender;
        }
        Stats stats = StatsManager.getInstance().getStats(arg);
        sender.sendMessage(stats.toLoreString());
    }

    @Subcommand("message")
    public void testMessage(Player sender){
        sender.sendMessage(ChatColor.AQUA + "SCAM!!!");
    }

    @Subcommand("add")
    public void add(Player sender, StatType type, double amount){
        Stats stats = StatsManager.getInstance().getStats(sender);
        stats.get(type).add(amount);
    }

    @Subcommand("set")
    public void set(Player sender, StatType type, double amount){
        Stats stats = StatsManager.getInstance().getStats(sender);
        stats.get(type).setValue(amount);
    }
}
