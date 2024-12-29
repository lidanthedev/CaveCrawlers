package me.lidan.cavecrawlers.skills;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class Skill implements ConfigurationSerializable {
    @Getter @Setter
    private static List<Double> xpToLevelList = new ArrayList<>();
    private SkillType type;
    private int level;
    private double xp;
    private double xpToLevel = 100;
    private double totalXp;

    public Skill(SkillType type, int level) {
        this.type = type;
        this.level = level;
    }

    public Skill(SkillType type, int level, double xp, double xpToLevel, double totalXp) {
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

    public int levelUp() {
        int leveled = 0;
        while (xp >= xpToLevel && level < xpToLevelList.size()){
            level++;
            xp -= xpToLevel;
            if (xp < 0){
                xp = 0;
            }
            xpToLevel = xpToLevelList.get(level);
            leveled++;
        }
        return leveled;
    }

    public Stats getStats(){
        Stats stats = new Stats(true);
        for (StatType statType : type.getStatType()) {
            stats.add(statType, level);
        }
        return stats;
    }

    public void sendLevelUpMessage(Player player){
        String skillName = StringUtils.setTitleCase(type.name());
        player.sendMessage(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "------------------------------------------");
        player.sendMessage("    " + ChatColor.AQUA + ChatColor.BOLD + "SKILL LEVEL UP " + ChatColor.RESET + ChatColor.DARK_AQUA + skillName + " " + ChatColor.DARK_GRAY + (this.level - 1) + ChatColor.DARK_GRAY + ChatColor.BOLD + "➡" + ChatColor.DARK_AQUA + this.level);
        player.sendMessage("");
        player.sendMessage("    " + ChatColor.GREEN + ChatColor.BOLD + "REWARDS");
        for (StatType statType : type.getStatType()) {
            player.sendMessage("       " + ChatColor.DARK_GRAY + "+" + statType.getColor() + "1 " + statType.getFormatName());
        }
        player.sendMessage(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "------------------------------------------");

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        return Map.of(
                "type", type.name(),
                "level", level,
                "xp", xp,
                "xpToLevel", xpToLevel,
                "totalXp", totalXp
        );
    }

    public static Skill deserialize(Map<String, Object> map) {
        Skill skill = new Skill(
                SkillType.valueOf((String) map.get("type")),
                0,
                (double) map.get("totalXp"),
                xpToLevelList.get(0),
                (double) map.get("totalXp")
        );
        skill.levelUp();
        return skill;
    }
}
