package me.lidan.cavecrawlers.skills;

import lombok.Getter;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.stats.ActionBarManager;
import me.lidan.cavecrawlers.storage.PlayerDataManager;
import me.lidan.cavecrawlers.utils.CustomConfig;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SkillXpManager {
    private static final String DIR_NAME = "skills";
    private static SkillXpManager instance;
    private File dir = new File(CaveCrawlers.getInstance().getDataFolder(), DIR_NAME);
    @Getter
    private Map<SkillType, CustomConfig> skillConfigs = new HashMap<>();
    private final CaveCrawlers plugin = CaveCrawlers.getInstance();

    public SkillXpManager() {
        if (!dir.exists()) {
            dir.mkdirs();
        }
        for (String name : SkillType.names()) {
            File file = new File(dir, name + ".yml");
            skillConfigs.put(SkillType.valueOf(name), new CustomConfig(file));
        }
        instance = this;
    }


    public void tryGiveXp(SkillType skillType, String reason, String material, Player player) {
        CustomConfig config = getConfig(skillType);
        if (!config.contains(reason)) {
            return;
        }
        ConfigurationSection map = config.getConfigurationSection(reason);
        if (map != null && map.contains(material)) {
            double xp = map.getDouble(material);
            giveXp(player, skillType, xp, true);
        }
    }

    public void tryGiveXp(SkillType skillType, String reason, Material material, Player player) {
        tryGiveXp(skillType, reason, material.name(), player);
    }

    public void tryGiveXp(String reason, String material, Player player) {
        for (SkillType skillType : skillConfigs.keySet()) {
            tryGiveXp(skillType, reason, material, player);
        }
    }

    public void tryGiveXp(String reason, Material material, Player player) {
        tryGiveXp(reason, material.name(), player);
    }

    public void giveXp(Player player, SkillType skillType, double xp, boolean showMessage) {
        Skills skills = PlayerDataManager.getInstance().getSkills(player);
        Skill skill = skills.get(skillType);
        skill.addXp(xp);
        String skillName = StringUtils.setTitleCase(skillType.name());
        skills.tryLevelUp(skillType);
        if (showMessage) {
            String message = ChatColor.DARK_AQUA + "+" + xp + " " + skillName + " (" + Math.floor(skill.getXp() / skill.getXpToLevel() * 1000d) / 10d + "%)";
            ActionBarManager.getInstance().actionBar(player, message);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 2);
        }
    }

    public CustomConfig getConfig(SkillType type) {
        return skillConfigs.get(type);
    }

    public static SkillXpManager getInstance() {
        if (instance == null) {
            instance = new SkillXpManager();
        }
        return instance;
    }
}
