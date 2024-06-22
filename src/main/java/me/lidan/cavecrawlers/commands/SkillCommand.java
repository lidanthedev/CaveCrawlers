package me.lidan.cavecrawlers.commands;

import me.lidan.cavecrawlers.gui.SkillsGui;
import me.lidan.cavecrawlers.skills.SkillType;
import me.lidan.cavecrawlers.skills.Skills;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.storage.PlayerDataManager;
import me.lidan.cavecrawlers.utils.CustomConfig;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.List;

@Command({"skills", "myskills", "skilladmin"})
@CommandPermission("cavecrawlers.skills")
public class SkillCommand {

    private final PlayerDataManager playerDataManager;

    public SkillCommand() {
        playerDataManager = PlayerDataManager.getInstance();
    }

    @Subcommand("list")
    public void listStats(Player sender, @Optional Player arg){
        if(arg == null) {
            arg = sender;
        }
        Skills skills = playerDataManager.getSkills(arg);
        sender.sendMessage(skills.toFormatString());
    }

    @Subcommand("lore")
    public void loreStats(Player sender, @Optional Player arg){
        if(arg == null) {
            arg = sender;
        }
        Stats stats = playerDataManager.getStatsFromSkills(arg);
        List<String> lore = stats.toLoreList();
        for (String line : lore) {
            sender.sendMessage(line);
        }
    }

    @Subcommand("addxp")
    public void addXp(Player sender, SkillType type, int amount){
        Skills skills = playerDataManager.getSkills(sender);
        skills.addXp(type, amount);
        sender.sendMessage("add xp to %s".formatted(type));
    }

    @Subcommand("set")
    public void set(Player sender, SkillType type, int amount){
        Skills stats = playerDataManager.getSkills(sender);
        stats.get(type).setLevel(amount);
        sender.sendMessage(ChatColor.GREEN + "set stat %s to %s".formatted(type, amount));
    }

    @Subcommand("test")
    public void test(Player sender){
        // save skills to custom config
        Skills skills = playerDataManager.getSkills(sender);
        CustomConfig config = new CustomConfig("testskill.yml");
        config.set("skills", skills);
        config.save();
        // load skills from custom config
        Skills loadedSkills = (Skills) config.get("skills");
        if (loadedSkills == null) {
            sender.sendMessage("Failed to load skills from config file.");
            return;
        }
        sender.sendMessage(loadedSkills.toFormatString());
    }

    @Subcommand("testLoad")
    public void testLoad(Player sender){
        // load skills from custom config
        CustomConfig config = new CustomConfig("testskill.yml");
        Skills loadedSkills = (Skills) config.get("skills");
        if (loadedSkills == null) {
            sender.sendMessage("Failed to load skills from config file.");
            return;
        }
        sender.sendMessage(loadedSkills.toFormatString());
    }

    @Subcommand("gui")
    public void openGui(Player sender){
        new SkillsGui(sender).open();
    }
}
