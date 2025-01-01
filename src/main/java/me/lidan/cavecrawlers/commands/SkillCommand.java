package me.lidan.cavecrawlers.commands;

import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.gui.SkillsGui;
import me.lidan.cavecrawlers.skills.*;
import me.lidan.cavecrawlers.stats.Stat;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.storage.PlayerDataManager;
import me.lidan.cavecrawlers.utils.CustomConfig;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.HashMap;
import java.util.List;

@Slf4j
@Command({"skills", "myskills", "skilladmin"})
@CommandPermission("cavecrawlers.skills")
public class SkillCommand {

    public static final CustomConfig TEST_SKILL_CONFIG = new CustomConfig("testskill.yml");
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
    public void addXp(Player sender, SkillInfo type, double amount) {
        SkillsManager skillsManager = SkillsManager.getInstance();
        Skills skills = playerDataManager.getSkills(sender);
        skillsManager.giveXp(sender, type, amount, true);
        sender.sendMessage("add xp to %s".formatted(type.getName()));
    }

    @Subcommand("set")
    public void set(Player sender, SkillInfo type, int amount) {
        Skills stats = playerDataManager.getSkills(sender);
        stats.get(type).setLevel(amount);
        sender.sendMessage(ChatColor.GREEN + "set stat %s to %s".formatted(type.getName(), amount));
    }

    @Subcommand("test")
    public void test(Player sender){
        // save skills to custom config
        Skills skills = playerDataManager.getSkills(sender);
        CustomConfig config = TEST_SKILL_CONFIG;
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
        CustomConfig config = TEST_SKILL_CONFIG;
        Skills loadedSkills = (Skills) config.get("skills");
        if (loadedSkills == null) {
            sender.sendMessage("Failed to load skills from config file.");
            return;
        }
        sender.sendMessage(loadedSkills.toFormatString());
    }

    @Subcommand("gui")
    @DefaultFor("skills")
    public void openGui(Player sender){
        new SkillsGui(sender).open();
    }

    @Subcommand("testBetaSkill")
    public void testBetaSkill(Player sender, @Default("1") int level) {
        HashMap<Integer, List<SkillReward>> rewards = new HashMap<>();
        rewards.put(1, List.of(new StatSkillReward(new Stat(StatType.STRENGTH, 10))));
        rewards.put(5, List.of(new StatSkillReward(new Stat(StatType.STRENGTH, 20))));
        SkillInfo skillInfo = new SkillInfo("BetaSkill", rewards, true);
        skillInfo.getRewards().get(level).forEach(reward -> {
            reward.applyReward(sender);
            sender.sendMessage("Applied reward %s".formatted(reward));
            sender.sendMessage("Stats: %s".formatted(skillInfo.getStats(level).toFormatString()));
        });
        TEST_SKILL_CONFIG.set("BetaSkill", skillInfo);
        TEST_SKILL_CONFIG.save();
    }

    @Subcommand("testBetaSkillLoad")
    public void testBetaSkillLoad(Player sender) {
        SkillInfo skillInfo = (SkillInfo) TEST_SKILL_CONFIG.get("BetaSkill");
        if (skillInfo == null) {
            sender.sendMessage("Failed to load skill from config file.");
            return;
        }
        log.info("Loaded skill: {}", skillInfo);
    }
}
