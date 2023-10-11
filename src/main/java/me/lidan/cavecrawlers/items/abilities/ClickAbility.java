package me.lidan.cavecrawlers.items.abilities;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public abstract class ClickAbility extends ItemAbility implements Listener {

    private Action[] allowedActions;

    public ClickAbility(String name, String description, double cost, long cooldown) {
        this(name, description, cost, cooldown, Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK);
    }

    public ClickAbility(String name, String description, double cost, long cooldown, Action... allowedActions) {
        super(name, description, cost, cooldown);
        this.allowedActions = allowedActions;
    }

    public void setAllowedActions(Action... allowedActions){
        this.allowedActions = allowedActions;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(event.getHand() == EquipmentSlot.OFF_HAND) return;
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hasAbility(hand)){
            Action action = event.getAction();
            for (Action allowedAction : allowedActions) {
                if (action == allowedAction){
                    activateAbility(player);
                }
            }
        }
    }
}
