package me.lidan.cavecrawlers.listeners;

import io.lumine.mythic.core.mobs.ActiveMob;
import lombok.Getter;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.skills.Skill;
import me.lidan.cavecrawlers.skills.SkillType;
import me.lidan.cavecrawlers.skills.Skills;
import me.lidan.cavecrawlers.stats.ActionBarManager;
import me.lidan.cavecrawlers.storage.PlayerDataManager;
import me.lidan.cavecrawlers.utils.CustomConfig;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


// TODO: split this class to manager and listener
public class XpGainingListener implements Listener {
    private static final String DIR_NAME = "skills";
    private static XpGainingListener instance;
    private File dir = new File(CaveCrawlers.getInstance().getDataFolder(), DIR_NAME);
    @Getter
    private Map<SkillType, CustomConfig> skillConfigs = new HashMap<>();
    private final CaveCrawlers plugin = CaveCrawlers.getInstance();

    public XpGainingListener() {
        if (!dir.exists()) {
            dir.mkdirs();
        }
        for (String name : SkillType.names()) {
            File file = new File(dir, name + ".yml");
            skillConfigs.put(SkillType.valueOf(name), new CustomConfig(file));
        }
        instance = this;
    }

    public CustomConfig getConfig(SkillType type) {
        return skillConfigs.get(type);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material material = block.getType();
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }
        String reason = "break";
        tryGiveXp(reason, material, player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        Location location = event.getBlock().getLocation();
        location.getWorld().getNearbyEntities(location, 10, 10, 10).stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .forEach(player -> {
                    ItemStack modifier = event.getContents().getIngredient();
                    if (modifier == null) {
                        return;
                    }
                    tryGiveXp("brew", modifier.getType(), player);
                });
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) {
            return;
        }
        Player player = event.getEntity().getKiller();
        String reason = "kill";
        String type = event.getEntityType().name();
        if (plugin.getMythicBukkit() != null) {
            ActiveMob activeMob = plugin.getMythicBukkit().getAPIHelper().getMythicMobInstance(event.getEntity());
            if (activeMob != null) {
                type = activeMob.getType().getInternalName();
            }
        }
        tryGiveXp(reason, type, player);

    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Entity caught = event.getCaught();
        if (!(caught instanceof Item item)) {
            return;
        }
        Player player = event.getPlayer();
        tryGiveXp("fish", item.getItemStack().getType(), player);
    }

    public void tryGiveXp(SkillType skillType, String reason, String material, Player player) {
        CustomConfig config = getConfig(skillType);
        if (!config.contains(reason)){
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

    public static XpGainingListener getInstance() {
        if (instance == null) {
            throw new IllegalStateException("XpGainingListener has not been initialized yet!");
        }
        return instance;
    }
}
