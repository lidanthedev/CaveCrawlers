package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.damage.DamageManager;
import me.lidan.cavecrawlers.damage.FinalDamageCalculation;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class BoomAbility extends ItemAbility implements Listener {
    public BoomAbility() {
        super("BOOM BOOM", "Does BOOM", 0, 100);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hasAbility(hand)){
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK){
                activateAbility(player);
            }
        }
    }

    @Override
    protected void useAbility(Player player) {
        DamageManager damageManager = DamageManager.getInstance();
        List<Entity> nearbyEntities = player.getNearbyEntities(3, 3, 3);
        double damage = 10000;
        FinalDamageCalculation calculation = new FinalDamageCalculation(damage, false);
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Mob mob){
                calculation.damage(player, mob);
            }
        }
    }

    @Override
    public void abilityFailedCooldown(Player player) {
        // silent cooldown
    }
}
