package me.lidan.cavecrawlers.skills;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Data
public class Skill implements ConfigurationSerializable {
    @Getter @Setter
    private static List<Double> defaultXpToLevelList = new ArrayList<>(); // updated from main config
    private SkillInfo type;
    private int level;
    private double xp;
    private double xpToLevel = 100;
    private double totalXp;

    private UUID uuid;

    public Skill(SkillInfo type, int level) {
        this.type = type;
        this.level = level;
    }

    public Skill(SkillInfo type, int level, double xp, double xpToLevel, double totalXp) {
        this.type = type;
        this.level = level;
        this.xp = xp;
        this.xpToLevel = xpToLevel;
        this.totalXp = totalXp;
    }

    public void addXp(double amount){
        xp += amount;
        totalXp += amount;
    }

    public void setXp(double xp) {
        this.totalXp = xp - this.xp + totalXp;
        this.xp = xp;
    }

    public int levelUp(boolean withRewards) {
        Player player = getPlayer();
        int leveled = 0;
        while (xp >= xpToLevel && level + 1 < type.getXpToLevelList().size() && level < type.getMaxLevel()) {
            level++;
            xp -= xpToLevel;
            if (xp < 0){
                xp = 0;
            }
            xpToLevel = type.getXpToLevelList().get(level);
            if (withRewards) {
                List<SkillReward> rewards = type.getRewards(level);
                for (SkillReward reward : rewards) {
                    reward.applyReward(player);
                }
            }
            leveled++;
        }
        return leveled;
    }

    public Stats getStats(){
        Stats stats = type.getStats(level);
        if (stats == null) {
            stats = new Stats(true);
        }
        return stats;
    }

    public void sendLevelUpMessage(Player player){
        String skillName = StringUtils.setTitleCase(type.getName());
        player.sendMessage(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "------------------------------------------");
        player.sendMessage("    " + ChatColor.AQUA + ChatColor.BOLD + "SKILL LEVEL UP " + ChatColor.RESET + ChatColor.DARK_AQUA + skillName + " " + ChatColor.DARK_GRAY + (this.level - 1) + ChatColor.DARK_GRAY + ChatColor.BOLD + "➡" + ChatColor.DARK_AQUA + this.level);
        player.sendMessage("");
        player.sendMessage("    " + ChatColor.GREEN + ChatColor.BOLD + "REWARDS");
        List<SkillReward> rewards = type.getRewards(level);
        for (SkillReward reward : rewards) {
            player.sendMessage(Component.text("    ").append(reward.getRewardMessage()));
        }
        player.sendMessage(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "------------------------------------------");

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        return Map.of(
                "type", type.getId(),
                "level", level,
                "xp", xp,
                "xpToLevel", xpToLevel,
                "totalXp", totalXp
        );
    }

    public static Skill deserialize(Map<String, Object> map) {
        String type = (String) map.get("type");
        SkillInfo skillInfo = SkillsManager.getInstance().getSkillInfo(type);
        Skill skill = new Skill(
                skillInfo,
                0,
                (double) map.get("totalXp"),
                skillInfo.getXpToLevelList().get(0),
                (double) map.get("totalXp")
        );
        skill.levelUp(false);
        return skill;
    }
}
