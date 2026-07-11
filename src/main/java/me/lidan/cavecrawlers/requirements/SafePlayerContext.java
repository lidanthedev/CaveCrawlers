package me.lidan.cavecrawlers.requirements;

import lombok.Getter;
import me.lidan.cavecrawlers.skills.Skills;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.storage.PlayerSkillsManager;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
public class SafePlayerContext {
    private final UUID uniqueId;
    private final String name;
    private final String displayName;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final double health;
    private final double maxHealth;
    private final int foodLevel;
    private final int level;
    private final boolean op;
    private final boolean sneaking;
    private final boolean sprinting;
    private final String gameMode;
    private final Stats stats;
    private final Skills skills;

    public SafePlayerContext(Player player) {
        this.uniqueId = player.getUniqueId();
        this.name = player.getName();
        this.displayName = player.getDisplayName();
        this.worldName = player.getWorld().getName();
        this.x = player.getLocation().getX();
        this.y = player.getLocation().getY();
        this.z = player.getLocation().getZ();
        this.health = player.getHealth();
        this.maxHealth = player.getMaxHealth();
        this.foodLevel = player.getFoodLevel();
        this.level = player.getLevel();
        this.op = player.isOp();
        this.sneaking = player.isSneaking();
        this.sprinting = player.isSprinting();
        this.gameMode = player.getGameMode().name();
        this.stats = StatsManager.getInstance().getStats(player);
        this.skills = PlayerSkillsManager.getInstance().getSkills(player);
    }

}
