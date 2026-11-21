package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.ToString;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@ToString(callSuper = true)
public abstract class ClickAbility extends ItemAbility implements Listener {
    public static final Action[] DEFAULT_CLICK_ACTIONS = {Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK};
    @Getter
    private Action[] allowedActions;
    private String displayAction;

    public ClickAbility(String name, String description, double cost, long cooldown) {
        this(name, description, cost, cooldown, DEFAULT_CLICK_ACTIONS);
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
        if (map.has("displayAction")) {
            ability.displayAction = map.get("displayAction").getAsString();
        }
        return ability;
    }

    String getDisplayAction() {
        if (displayAction != null) {
            return displayAction;
        }
        boolean right = false;
        boolean left = false;
        for (Action action : allowedActions) {
            right |= action.isRightClick();
            left |= action.isLeftClick();
        }
        if (right == left) {
            return DisplayAction.ANY_CLICK.getValue();
        }
        return right ? DisplayAction.RIGHT_CLICK.getValue() : DisplayAction.LEFT_CLICK.getValue();
    }

    @Override
    public List<String> toList() {
        List<String> list = new ArrayList<>();
        list.add(ChatColor.GOLD + "Item Ability: " + getName() + " " + ChatColor.YELLOW + ChatColor.BOLD + getDisplayAction());
        addAbilityDescriptionToList(list);
        return list;
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
