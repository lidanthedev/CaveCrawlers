package me.lidan.cavecrawlers.commands;

import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.List;

@Command({"stats", "stat", "statadmin"})
@CommandPermission("cavecrawlers.stats")
public class StatCommand {

    private final StatsManager statsManager;

    public StatCommand() {
        statsManager = StatsManager.getInstance();
    }

    @Subcommand("list")
    public void listStats(Player sender, @Optional Player arg){
        if(arg == null) {
            arg = sender;
        }
        Stats stats = statsManager.getStats(arg);
        sender.sendMessage(stats.toFormatString());
    }

    @Subcommand("lore")
    public void loreStats(Player sender, @Optional Player arg){
        if(arg == null) {
            arg = sender;
        }
        Stats stats = statsManager.getStats(arg);
        List<String> lore = stats.toLoreList();
        for (String line : lore) {
            sender.sendMessage(line);
        }
    }

    @Subcommand("add")
    public void add(Player sender, StatType type, double amount){
        Stats stats = statsManager.getStats(sender);
        stats.get(type).add(amount);
        sender.sendMessage("add stat %s to %s".formatted(type, amount));
    }

    @Subcommand("set")
    public void set(Player sender, StatType type, double amount){
        Stats stats = statsManager.getStats(sender);
        stats.get(type).setValue(amount);
        sender.sendMessage(ChatColor.GREEN + "set stat %s to %s".formatted(type, amount));
    }

    @Subcommand("health")
    public void health(Player sender){
        sender.sendMessage("%s/%s".formatted(sender.getHealth(), sender.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
    }

    @Subcommand("apply")
    public void apply(Player sender){
        statsManager.applyStats(sender);
        sender.sendMessage("Applied stats!");
    }
}
