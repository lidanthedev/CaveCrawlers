package me.lidan.cavecrawlers.skills;

import lombok.Data;
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
    private static final List<Double> xpToLevelList = new ArrayList<>();
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

    public boolean levelUp() {
        boolean leveled = false;
        while (xp >= xpToLevel && level < 50){
            level++;
            xp -= xpToLevel;
            if (xp < 0){
                xp = 0;
            }
            xpToLevel = Math.pow(level, 2) + 100; // CHANGE LATER
            leveled = true;
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

    public void setValue(int amount) {
        level = amount;
    }

    public void add(int amount) {
        level += amount;
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
        return new Skill(
                SkillType.valueOf((String) map.get("type")),
                (int) map.get("level"),
                (double) map.get("xp"),
                (double) map.get("xpToLevel"),
                (double) map.get("totalXp")
        );
    }


}
