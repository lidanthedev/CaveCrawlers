package me.lidan.cavecrawlers.listeners;

import io.lumine.mythic.core.mobs.ActiveMob;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.skills.SkillsManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
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


public class SkillXpGainingListener implements Listener {
    private final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private final SkillsManager skillsManager = SkillsManager.getInstance();

    public SkillXpGainingListener() {
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
        skillsManager.tryGiveXp(reason, material, player);
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
                    skillsManager.tryGiveXp("brew", modifier.getType(), player);
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
        skillsManager.tryGiveXp(reason, type, player);

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
        skillsManager.tryGiveXp("fish", item.getItemStack().getType(), player);
    }
}
