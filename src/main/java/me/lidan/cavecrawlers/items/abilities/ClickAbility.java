package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.ToString;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

@ToString(callSuper = true)
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
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        ClickAbility ability = (ClickAbility) super.buildAbilityWithSettings(map);
        if (map.has("allowedActions")) {
            JsonArray actions = map.get("allowedActions").getAsJsonArray();
            if (actions != null) {
                Action[] allowedActions = new Action[actions.size()];
                for (int i = 0; i < actions.size(); i++) {
                    allowedActions[i] = Action.valueOf(actions.get(i).getAsString());
                }
                ability.setAllowedActions(allowedActions);
            }
        }
        return ability;
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
                    event.setCancelled(true);
                    activateAbility(event);
                    return;
                }
            }
        }
    }
}
