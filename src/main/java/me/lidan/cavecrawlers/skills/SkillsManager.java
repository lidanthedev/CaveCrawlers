package me.lidan.cavecrawlers.skills;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.api.SkillsAPI;
import me.lidan.cavecrawlers.objects.ConfigLoader;
import me.lidan.cavecrawlers.skills.db.SkillsDao;
import me.lidan.cavecrawlers.stats.ActionBarManager;
import me.lidan.cavecrawlers.utils.BoostedCustomConfig;
import me.lidan.cavecrawlers.utils.CustomConfig;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jdbi.v3.core.Jdbi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter
public class SkillsManager extends ConfigLoader<SkillInfo> implements SkillsAPI {
    private static final String DIR_NAME = "skills";
    private static SkillsManager instance;
    private final Map<String, CustomConfig> skillConfigs = new HashMap<>();
    private final Map<String, SkillInfo> skillInfoMap = new HashMap<>();
    private final ConcurrentHashMap<UUID, Skills> activeSkills = new ConcurrentHashMap<>();
    private final Plugin plugin;
    private Jdbi jdbi;
    private SkillsDao skillsDao;

    public SkillsManager(Jdbi jdbi, Plugin plugin) {
        super(SkillInfo.class, DIR_NAME);
        this.jdbi = jdbi;
        this.plugin = plugin;
        if (jdbi != null) {
            this.skillsDao = jdbi.onDemand(SkillsDao.class);
        }
        instance = this;
    }

    public static SkillsManager initialize(Jdbi jdbi, Plugin plugin) {
        instance = new SkillsManager(jdbi, plugin);
        return instance;
    }

    public static SkillsManager getInstance() {
        if (instance == null) {
            instance = new SkillsManager(null, CaveCrawlers.getInstance());
        }
        return instance;
    }

    public void setDatabase(Jdbi jdbi) {
        this.jdbi = jdbi;
        this.skillsDao = jdbi.onDemand(SkillsDao.class);
    }

    public Skills getSkills(Player player) {
        return getSkills(player.getUniqueId());
    }

    public Skills getSkills(UUID uuid) {
        return activeSkills.computeIfAbsent(uuid, this::createDefaultSkills);
    }

    public void setSkills(UUID uuid, Skills skills) {
        skills.setUuid(uuid);
        activeSkills.put(uuid, skills);
    }

    public void loadPlayerSync(UUID uuid) {
        if (skillsDao == null) {
            activeSkills.put(uuid, createDefaultSkills(uuid));
            return;
        }
        List<Skill> dbSkills = skillsDao.getSkills(uuid.toString());
        Skills skills = createDefaultSkills(uuid);
        for (Skill dbSkill : dbSkills) {
            SkillInfo type = getSkillInfo(dbSkill.getTypeId());
            if (type == null) {
                continue;
            }
            Skill skill = skills.get(type);
            skill.setLevel(dbSkill.getLevel());
            skill.setXp(dbSkill.getXp());
            skill.setTotalXp(dbSkill.getTotalXp());
            skill.setXpToLevel(resolveXpToLevel(type, dbSkill.getLevel()));
        }
        activeSkills.put(uuid, skills);
    }

    public void savePlayerAsync(UUID uuid) {
        savePlayerSnapshotAsync(uuid, true);
    }

    public void savePlayerDataAsync(UUID uuid) {
        savePlayerSnapshotAsync(uuid, false);
    }

    public void saveAllAsync() {
        for (UUID uuid : new ArrayList<>(activeSkills.keySet())) {
            savePlayerSnapshotAsync(uuid, false);
        }
    }

    public void saveAllAndWait() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (UUID uuid : new ArrayList<>(activeSkills.keySet())) {
            futures.add(savePlayerSnapshotFuture(uuid, false));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private CompletableFuture<Void> savePlayerSnapshotFuture(UUID uuid, boolean removeAfterSave) {
        Skills skills = activeSkills.get(uuid);
        if (skills == null || skillsDao == null) {
            if (removeAfterSave) {
                activeSkills.remove(uuid);
            }
            return CompletableFuture.completedFuture(null);
        }
        List<Skill> snapshot = toSnapshot(uuid, skills);
        CompletableFuture<Void> future = new CompletableFuture<>();
        Runnable saveTask = () -> {
            try {
                skillsDao.upsertSkills(uuid.toString(), snapshot);
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
                if (plugin != null) {
                    plugin.getLogger().severe("Failed to save skills for " + uuid + ": " + e.getMessage());
                }
            } finally {
                if (removeAfterSave) {
                    activeSkills.remove(uuid);
                }
            }
        };
        if (plugin != null && plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, saveTask);
        } else {
            CompletableFuture.runAsync(saveTask);
        }
        return future;
    }

    private void savePlayerSnapshotAsync(UUID uuid, boolean removeAfterSave) {
        savePlayerSnapshotFuture(uuid, removeAfterSave);
    }

    private Skills createDefaultSkills(UUID uuid) {
        Skills skills = new Skills();
        skills.setUuid(uuid);
        return skills;
    }

    private List<Skill> toSnapshot(UUID uuid, Skills skills) {
        List<Skill> snapshot = new ArrayList<>();
        for (Skill skill : skills) {
            if (skill.getType() == null || skill.getType().getId() == null) {
                continue;
            }
            Skill clone = new Skill(skill.getType(), skill.getLevel(), skill.getXp(), skill.getXpToLevel(), skill.getTotalXp());
            clone.setTypeId(skill.getType().getId());
            clone.setUuid(uuid);
            snapshot.add(clone);
        }
        return snapshot;
    }

    private double resolveXpToLevel(SkillInfo type, int level) {
        List<Double> levels = type.getXpToLevelList();
        if (levels == null || levels.isEmpty()) {
            return 100;
        }
        int index = Math.max(0, Math.min(level, levels.size() - 1));
        return levels.get(index);
    }

    @Override
    public void register(String key, SkillInfo value) {
        value.setId(key);
        skillInfoMap.put(key, value);
    }

    @Override
    public SkillInfo getSkillInfo(String key) {
        return skillInfoMap.get(key);
    }

    @Override
    public void tryGiveXp(SkillInfo skillType, SkillAction reason, String material, Player player) {
        List<SkillObjective> objectives = skillType.getActionObjectives().get(reason);
        if (objectives == null) {
            return;
        }
        World world = player.getWorld();
        List<SkillObjective> matches = new ArrayList<>();
        for (SkillObjective objective : objectives) {
            if (objective.getObjective().equalsIgnoreCase(material)) {
                if (!objective.getWorlds().isEmpty() && !objective.getWorlds().contains(world.getName())) {
                    continue;
                }
                matches.add(objective);
            }
        }
        if (matches.isEmpty()) {
            return;
        }
        // prefer the most specific objective
        SkillObjective objective = matches.get(0);
        for (SkillObjective match : matches) {
            if (match.getWorlds().size() > objective.getWorlds().size()) {
                objective = match;
            }
        }
        double xp = objective.getAmount();
        giveXp(player, skillType, xp, true);
    }

    public void tryGiveXp(SkillInfo skillType, SkillAction reason, Material material, Player player) {
        tryGiveXp(skillType, reason, material.name(), player);
    }

    @Override
    public void tryGiveXp(SkillAction reason, String material, Player player) {
        Skills skills = getSkills(player);
        for (Skill skill : skills) {
            SkillInfo skillType = skill.getType();
            tryGiveXp(skillType, reason, material, player);
        }
    }

    public Map<SkillInfo, List<SkillObjective>> getObjectivesMatching(SkillAction action, String material) {
        Map<SkillInfo, List<SkillObjective>> result = new HashMap<>();
        for (SkillInfo skillInfo : skillInfoMap.values()) {
            List<SkillObjective> objectives = skillInfo.getActionObjectives().get(action);
            if (objectives == null) {
                continue;
            }
            List<SkillObjective> matches = new ArrayList<>();
            for (SkillObjective objective : objectives) {
                if (objective.getObjective().equalsIgnoreCase(material)) {
                    matches.add(objective);
                }
            }
            if (!matches.isEmpty()) {
                result.put(skillInfo, matches);
            }
        }
        return result;
    }

    public void tryGiveXp(SkillAction reason, Material material, Player player) {
        tryGiveXp(reason, material.name(), player);
    }

    public void giveXp(Player player, SkillInfo skillType, double xp, boolean showMessage) {
        Skills playerSkills = getSkills(player);
        Skill skill = playerSkills.get(skillType);
        if (skill == null) {
            skill = new Skill(skillType, 0);
            playerSkills.set(skillType, skill);
        }
        SkillXpGainEvent event = new SkillXpGainEvent(player, skill, xp);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        skill.addXp(event.getXpGained());
        String skillName = StringUtils.setTitleCase(skillType.getName());
        playerSkills.tryLevelUp(skillType);
        if (showMessage) {
            Component component = MiniMessageUtils.miniMessage("<dark_aqua>+<xp> <skill-name> (<xp-percent>%)", Map.of("xp", StringUtils.valueOf(event.getXpGained()), "skill-name", skillName, "xp-percent", String.valueOf(Math.floor(skill.getXp() / skill.getXpToLevel() * 1000d) / 10d)));
            ActionBarManager.getInstance().showActionBar(player, component);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 2);
        }
    }

    public BoostedCustomConfig getConfig(SkillInfo type) {
        return getConfig(type.getName());
    }
}
