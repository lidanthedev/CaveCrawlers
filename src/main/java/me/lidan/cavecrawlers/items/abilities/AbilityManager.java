package me.lidan.cavecrawlers.items.abilities;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class AbilityManager {
    private static AbilityManager instance;
    private final Map<String, ItemAbility> abilityMap;

    public AbilityManager() {
        this.abilityMap = new HashMap<>();
    }

    public void registerAbility(String ID, ItemAbility ability){
        abilityMap.put(ID, ability);
    }

    public @Nullable ItemAbility getAbilityByID(String ID){
        return abilityMap.get(ID);
    }

    public @Nullable String getIDbyAbility(ItemAbility ability){
        for (String ID : abilityMap.keySet()) {
            ItemAbility itemAbility = abilityMap.get(ID);
            if (itemAbility == ability){
                return ID;
            }
        }
        return null;
    }

    public static AbilityManager getInstance() {
        if (instance == null){
            instance = new AbilityManager();
        }
        return instance;
    }
}
