package me.lidan.cavecrawlers.skills;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
public class SkillsManager extends ConfigLoader<SkillInfo> {
    private static final String DIR_NAME = "skills";
    private static SkillsManager instance;
    private Map<String, CustomConfig> skillConfigs = new HashMap<>();
    private Map<String, SkillInfo> skillInfoMap = new HashMap<>();
    private final CaveCrawlers plugin = CaveCrawlers.getInstance();

    public SkillsManager() {
        super(SkillInfo.class, DIR_NAME);
        instance = this;
    }

    @Override
    public void register(String key, SkillInfo value) {
        value.setId(key);
        skillInfoMap.put(key, value);
    }

    public SkillInfo getSkillInfo(String key) {
        return skillInfoMap.get(key);
    }


    public void tryGiveXp(SkillInfo skillType, SkillAction reason, String material, Player player) {
        List<SkillObjective> objectives = skillType.getActionObjectives().get(reason);
        if (objectives == null) {
            return;
        }
        for (SkillObjective objective : objectives) {
            if (objective.getObjective().equalsIgnoreCase(material)) {
                double xp = objective.getAmount();
                giveXp(player, skillType, xp, true);
            }
        }
    }

    public void tryGiveXp(SkillInfo skillType, SkillAction reason, Material material, Player player) {
        tryGiveXp(skillType, reason, material.name(), player);
    }

    public void tryGiveXp(SkillAction reason, String material, Player player) {
        Skills skills = PlayerDataManager.getInstance().getSkills(player);
        for (Skill skill : skills) {
            SkillInfo skillType = skill.getType();
            tryGiveXp(skillType, reason, material, player);
        }
    }

    public void tryGiveXp(SkillAction reason, Material material, Player player) {
        tryGiveXp(reason, material.name(), player);
    }

    public void giveXp(Player player, SkillInfo skillType, double xp, boolean showMessage) {
        Skills skills = PlayerDataManager.getInstance().getSkills(player);
        Skill skill = skills.get(skillType);
        if (skill == null) {
            skill = new Skill(skillType, 0);
            skills.set(skillType, skill);
        }
        skill.addXp(xp);
        String skillName = StringUtils.setTitleCase(skillType.getName());
        skills.tryLevelUp(skillType);
        if (showMessage) {
            Component component = MiniMessageUtils.miniMessageString("<dark_aqua>+<xp> <skill-name> (<xp-percent>%)", Map.of("xp", StringUtils.valueOf(xp), "skill-name", skillName, "xp-percent", String.valueOf(Math.floor(skill.getXp() / skill.getXpToLevel() * 1000d) / 10d)));
            ActionBarManager.getInstance().actionBar(player, component);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 2);
        }
    }

    public CustomConfig getConfig(SkillInfo type) {
        return getConfig(type.getName());
    }

    public static SkillsManager getInstance() {
        if (instance == null) {
            instance = new SkillsManager();
        }
        return instance;
    }
}
