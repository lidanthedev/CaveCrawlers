package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiClickAbility extends ClickAbility {

    private List<ClickAbility> abilities;

    public MultiClickAbility() {
        this(new ArrayList<>());
    }

    public MultiClickAbility(List<ClickAbility> abilities) {
        super("Multi Click Ability", "Do multiple click abilities at once", 0, 0);
        this.abilities = abilities;
        this.setAllowedActions(Action.values());
    }

    @Override
    protected boolean useAbility(PlayerEvent playerEvent) {
        if (!(playerEvent instanceof PlayerInteractEvent playerInteractEvent)) return false;
        for (ClickAbility ability : abilities) {
            if (Arrays.asList(ability.getAllowedActions()).contains(playerInteractEvent.getAction())) {
                ability.activateAbility(playerEvent);
            }
        }
        return false;
    }

    @Override
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        MultiClickAbility ability = (MultiClickAbility) super.buildAbilityWithSettings(map);
        ability.abilities = new ArrayList<>();
        if (map.has("abilities")) {
            JsonArray abilities = map.get("abilities").getAsJsonArray();
            for (int i = 0; i < abilities.size(); i++) {
                String abilityName = abilities.get(i).getAsString();
                ItemAbility itemAbility = AbilityManager.getInstance().getAbilityByID(abilityName);
                if (itemAbility instanceof ClickAbility clickAbility) {
                    ability.abilities.add(clickAbility);
                }
            }
        }
        // listen to every action a child uses, else children with non-default actions never fire
        if (!map.has("allowedActions")) {
            ability.setAllowedActions(ability.abilities.stream()
                    .flatMap(child -> Arrays.stream(child.getAllowedActions()))
                    .distinct()
                    .toArray(Action[]::new));
        }
        return ability;
    }

    @Override
    public List<String> toList() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < abilities.size(); i++) {
            list.addAll(abilities.get(i).toList());
            if (i < abilities.size() - 1) {
                list.add("");
            }
        }
        return list;
    }
}
