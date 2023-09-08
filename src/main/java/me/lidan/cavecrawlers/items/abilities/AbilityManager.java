package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.event.Listener;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class AbilityManager {
    private static AbilityManager instance;
    private final Map<String, ItemAbility> abilityMap;

    public AbilityManager() {
        this.abilityMap = new HashMap<>();
    }

    /**
     * register ability and if it's a Bukkit Listener it will also register it as Listener
     * @param ID Ability ID
     * @param ability the ability object
     */
    public void registerAbility(String ID, ItemAbility ability){
        abilityMap.put(ID, ability);
        if (ability instanceof Listener){
            CaveCrawlers.getInstance().registerEvent((Listener) ability);
        }
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
