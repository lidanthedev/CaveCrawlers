package me.lidan.cavecrawlers.skills;

import lombok.Getter;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.objects.ConfigLoader;
import me.lidan.cavecrawlers.stats.ActionBarManager;
import me.lidan.cavecrawlers.storage.PlayerDataManager;
import me.lidan.cavecrawlers.utils.CustomConfig;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SkillsManager extends ConfigLoader<SkillInfo> {
    private static final String DIR_NAME = "skills";
    private static SkillsManager instance;
    private File dir = new File(CaveCrawlers.getInstance().getDataFolder(), DIR_NAME);
    @Getter
    private Map<String, CustomConfig> skillConfigs = new HashMap<>();
    private Map<String, SkillInfo> skillInfoMap = new HashMap<>();
    private final CaveCrawlers plugin = CaveCrawlers.getInstance();

    public SkillsManager() {
        super(SkillInfo.class, "skills");
        instance = this;
    }

    @Override
    public void register(String key, SkillInfo value) {
        skillInfoMap.put(key, value);
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
        for (String skillName : skillConfigs.keySet()) {
            SkillType skillType = SkillType.valueOf(skillName);
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
            Component component = MiniMessageUtils.miniMessageString("<dark_aqua>+<xp> <skill-name> (<xp-percent>%)", Map.of("xp", String.valueOf(xp), "skill-name", skillName, "xp-percent", String.valueOf(Math.floor(skill.getXp() / skill.getXpToLevel() * 1000d) / 10d)));
            ActionBarManager.getInstance().actionBar(player, component);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 2);
        }
    }

    public CustomConfig getConfig(SkillType type) {
        return skillConfigs.get(type);
    }

    public static SkillsManager getInstance() {
        if (instance == null) {
            instance = new SkillsManager();
        }
        return instance;
    }
}
