package me.lidan.cavecrawlers.items.abilities;

import lombok.ToString;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import java.util.Arrays;
import java.util.Map;

public abstract class ClickAbility extends ItemAbility implements Listener {

    private Action[] allowedActions;

    public ClickAbility(String name, String description, double cost, long cooldown) {
        this(name, description, cost, cooldown, Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK);
    }

    public ClickAbility(String name, String description, double cost, long cooldown, Action... allowedActions) {
        super(name, description, cost, cooldown);
        this.allowedActions = allowedActions;
    }

    @Override
    public ItemAbility buildAbilityWithSettings(Map<String, Object> map) {
        ClickAbility ability = (ClickAbility) super.buildAbilityWithSettings(map);
        String[] actions = (String[]) map.get("allowedActions");
        if (actions != null) {
            Action[] allowedActions = new Action[actions.length];
            for (int i = 0; i < actions.length; i++) {
                allowedActions[i] = Action.valueOf(actions[i]);
            }
            ability.setAllowedActions(allowedActions);
        }
        return ability;
    }

    public void setAllowedActions(Action... allowedActions){
        this.allowedActions = allowedActions;
    }

    @Override
    public String toString() {
        return "ClickAbility{" +
                "allowedActions=" + Arrays.toString(allowedActions) +
                "} " + super.toString();
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
                    activateAbility(event);
                }
            }
        }
    }
}
