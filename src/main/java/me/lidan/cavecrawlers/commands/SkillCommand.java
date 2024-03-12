package me.lidan.cavecrawlers.commands;

import me.lidan.cavecrawlers.skills.SkillType;
import me.lidan.cavecrawlers.skills.Skills;
import me.lidan.cavecrawlers.skills.SkillsManager;
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

@Command({"skills", "myskills", "skilladmin"})
@CommandPermission("cavecrawlers.skills")
public class SkillCommand {

    private final SkillsManager skillsManager;

    public SkillCommand() {
        skillsManager = SkillsManager.getInstance();
    }

    @Subcommand("list")
    public void listStats(Player sender, @Optional Player arg){
        if(arg == null) {
            arg = sender;
        }
        Skills skills = skillsManager.getSkills(arg);
        sender.sendMessage(skills.toFormatString());
    }

    @Subcommand("lore")
    public void loreStats(Player sender, @Optional Player arg){
        if(arg == null) {
            arg = sender;
        }
        Stats stats = skillsManager.getStats(arg);
        List<String> lore = stats.toLoreList();
        for (String line : lore) {
            sender.sendMessage(line);
        }
    }

    @Subcommand("add")
    public void add(Player sender, SkillType type, int amount){
        Skills skills = skillsManager.getSkills(sender);
        skills.get(type).add(amount);
        sender.sendMessage("add stat %s to %s".formatted(type, amount));
    }

    @Subcommand("addxp")
    public void addXp(Player sender, SkillType type, int amount){
        Skills skills = skillsManager.getSkills(sender);
        skills.addXp(type, amount);
        sender.sendMessage("add xp to %s".formatted(type));
    }

    @Subcommand("set")
    public void set(Player sender, SkillType type, int amount){
        Skills stats = skillsManager.getSkills(sender);
        stats.get(type).setValue(amount);
        sender.sendMessage(ChatColor.GREEN + "set stat %s to %s".formatted(type, amount));
    }

    @Subcommand("health")
    public void health(Player sender){
        sender.sendMessage("%s/%s".formatted(sender.getHealth(), sender.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
    }
}
