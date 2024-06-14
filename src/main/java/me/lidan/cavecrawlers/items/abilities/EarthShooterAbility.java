package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonObject;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.utils.BukkitUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class EarthShooterAbility extends ClickAbility {
    private int radius = 5;

    private final Map<UUID, List<ArmorStand>> playersBlocks = new HashMap<>();

    public EarthShooterAbility() {
        super("Earth Shooter", "Takes the blocks around you shoots them towards your enemies!", 350, 1500, Action.values());
    }

    @Override
    protected boolean useAbility(PlayerEvent event) {
        Player player = event.getPlayer();

        if (!(event instanceof PlayerInteractEvent e)) {
            return false;
        }

        if (playersBlocks.containsKey(player.getUniqueId()) && (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK)) {
            List<ArmorStand> armorStands = playersBlocks.get(player.getUniqueId());

            for (ArmorStand armorStand : armorStands) {
                armorStand.setVelocity(player.getLocation().getDirection().subtract(new Vector(0, 0.5, 0)));
            }

            playersBlocks.remove(player.getUniqueId());
        }else if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block lowestBlock = getLowestBlock(player.getLocation());
            List<Block> blocks = BukkitUtils.loopBlocks(lowestBlock.getLocation(), radius);

            List<ArmorStand> armorStands = new ArrayList<>();
            for (Block block : blocks) {
                if (block.getY() != lowestBlock.getY()) continue;

                ArmorStand armorStand = player.getWorld().spawn(block.getLocation().add(0, 2, 0), ArmorStand.class);
                armorStand.setInvulnerable(true);
                armorStand.setVisible(true);
                armorStand.addScoreboardTag("EarthShooter");

                ItemStack head = new ItemStack(block.getType());
                head.setData(block.getState().getData());
                armorStand.getEquipment().setHelmet(head);

                armorStand.setVelocity(new Vector(0, 1, 0));

                armorStands.add(armorStand);
            }
            playersBlocks.put(player.getUniqueId(), armorStands);

            long started = System.currentTimeMillis();
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (armorStands.isEmpty() || System.currentTimeMillis() - started >= 10_000) {
                        this.cancel();
                        return;
                    }

                    if (armorStands.get(0).getVelocity().getY() < 0) {
                        armorStands.parallelStream().forEach(a -> a.setGravity(false));
                    }
                }
            }.runTaskTimer(CaveCrawlers.getInstance(), 0L, 1L);
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
