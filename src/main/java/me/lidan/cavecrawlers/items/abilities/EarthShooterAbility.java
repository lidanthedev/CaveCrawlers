package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonObject;
import me.lidan.cavecrawlers.utils.BukkitUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

public class EarthShooterAbility extends ClickAbility {
    private int radius = 5;

    private final Map<UUID, List<BlockDisplay>> playersBlocks = new HashMap<>();

    public EarthShooterAbility() {
        super("Earth Shooter", "Takes the blocks around you shoots them towards your enemies!", 350, 1500, Action.values());
    }

    @Override
    protected boolean useAbility(PlayerEvent event) {
        Player player = event.getPlayer();

        if (!(event instanceof PlayerInteractEvent e)) {
            return false;
        }
        if (!player.getName().equalsIgnoreCase("LidanTheGamer")) {
            // until maxi fixes this ability it will be disabled
            player.sendMessage("You are not allowed to use this ability!");
            return false;
        }

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block lowestBlock = getLowestBlock(player.getLocation());
            List<Block> blocks = BukkitUtils.loopBlocks(lowestBlock.getLocation(), radius);

            List<BlockDisplay> blockDisplays = new ArrayList<>();
            for (Block block : blocks) {
                if (block.getY() != lowestBlock.getY()) continue;

                BlockDisplay blockDisplay = player.getWorld().spawn(block.getLocation().add(0, 1, 0), BlockDisplay.class);
                blockDisplay.setBlock(block.getBlockData());
                blockDisplay.addScoreboardTag("EarthShooter");

                blockDisplays.add(blockDisplay);
            }
            playersBlocks.put(player.getUniqueId(), blockDisplays);
        }
        return true;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof ArmorStand armorStand && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (!armorStand.getScoreboardTags().contains("EarthShooter")) return;

            armorStand.remove();
            event.setCancelled(true);
        }
    }

    public static Block getLowestBlock(Location location) {
        for (int i = 0; i < location.getY(); i++) {
            Block block = location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY() - i, location.getBlockZ());
            if (block.getType().isSolid()) {
                return block;
            }
        }
        return location.getBlock();
    }

    @Override
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        EarthShooterAbility ability = (EarthShooterAbility) super.buildAbilityWithSettings(map);
        if (map.has("radius")) {
            ability.radius = map.get("radius").getAsInt();
        }
        return ability;
    }
}
