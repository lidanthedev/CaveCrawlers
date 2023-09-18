package me.lidan.cavecrawlers.items.abilities;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TargetAbility extends ItemAbility implements Listener {

    private final Map<UUID, Mob> mobMap = new HashMap<>();
    private final Map<UUID, Event> eventMap = new HashMap<>();

    public TargetAbility(String name, String description, double cost, long cooldown) {
        super(name, description, cost, cooldown);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hasAbility(hand)){
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK){
                eventMap.put(player.getUniqueId(), event);
                activateAbility(player);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hasAbility(hand)){
            eventMap.put(player.getUniqueId(), event);
            activateAbility(player);
        }
    }

    @Override
    protected void useAbility(Player player) {
        Event event = eventMap.get(player.getUniqueId());
        if (event instanceof PlayerInteractAtEntityEvent interactAtEntityEvent){
            if (interactAtEntityEvent.getRightClicked() instanceof Mob mob){
                if (player.isSneaking()){
                    mob.setGlowing(true);
                    mobMap.put(player.getUniqueId(), mob);
                }
            }
        } else if (event instanceof PlayerInteractEvent) {
            if (!player.isSneaking()){
                Mob mob = mobMap.get(player.getUniqueId());
                if (mob != null && !mob.isDead()){
                    mob.getWorld().getNearbyEntities(mob.getLocation(), 20,20,20).forEach(entity -> {
                        if (entity instanceof Mob enemy){
                            if (enemy != mob){
                                enemy.setTarget(mob);
                            }
                        }
                    });
                }
            }
        }
    }
}
