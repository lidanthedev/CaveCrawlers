package me.lidan.cavecrawlers.commands;

import com.cryptomorin.xseries.XAttribute;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
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
    public void listStats(Player sender, @Default("me") Player arg) {
        Stats stats = statsManager.getStats(arg);
        sender.sendMessage(stats.toFormatString());
    }

    @Subcommand("lore")
    public void loreStats(Player sender, @Default("me") Player arg) {
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
    public void add(Player sender, StatType type, double amount, @Default("me") Player arg) {
        Stats stats = statsManager.getStatsAdder(arg);
        stats.get(type).add(amount);
        sender.sendMessage("add stat %s to %s for player %s".formatted(type.name(), amount, arg.getName()));
    }

    @Subcommand("set")
    public void set(Player sender, StatType type, double amount, @Default("me") Player arg) {
        Stats stats = statsManager.getStatsAdder(sender);
        stats.get(type).setValue(amount);
        sender.sendMessage(ChatColor.GREEN + "set stat %s to %s for player %s".formatted(type.name(), amount, arg.getName()));
    }

    @Subcommand("health")
    public void health(Player sender){
        sender.sendMessage("%s/%s".formatted(sender.getHealth(), sender.getAttribute(XAttribute.MAX_HEALTH.get()).getValue()));
    }

    @Subcommand("apply")
    public void apply(Player sender, @Default("me") Player arg) {
        statsManager.applyStats(arg);
        sender.sendMessage("Applied stats for player %s!".formatted(arg.getName()));
    }
}
