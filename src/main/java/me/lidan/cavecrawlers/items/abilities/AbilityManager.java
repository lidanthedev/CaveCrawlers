package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.event.Listener;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class AbilityManager {
    private static AbilityManager instance;
    @Getter
    private final Map<String, ItemAbility> abilityMap;
    private final JSONParser parser = new JSONParser();
    private final CaveCrawlers plugin;


    public AbilityManager() {
        this.abilityMap = new HashMap<>();
        this.plugin = CaveCrawlers.getInstance();
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
        if (ID == null){
            return null;
        }
        if (abilityMap.containsKey(ID)){
            return abilityMap.get(ID);
        }
        if (ID.contains("{")){
            String IDWithoutSettings = ID.substring(0, ID.indexOf("{"));
            String settings = ID.substring(ID.indexOf("{"));
            if (abilityMap.containsKey(IDWithoutSettings)){
                ItemAbility ability = abilityMap.get(IDWithoutSettings);
                JsonObject jo;
                try {
                    jo = (JsonObject) JsonParser.parseString(settings);
                } catch (JsonSyntaxException e) {
                    plugin.getLogger().warning("Failed to parse settings for ability: " + ID);
                    e.printStackTrace();
                    return null;
                }
                plugin.getLogger().info("Building " + IDWithoutSettings + " using settings: " + jo.toString());
                ItemAbility itemAbility = ability.buildAbilityWithSettings(jo);
                if (itemAbility == null){
                    plugin.getLogger().warning("Failed to build ability with settings for ability: " + ID + " Using Default");
                    return abilityMap.get(IDWithoutSettings);
                }
                registerAbility(ID, itemAbility);
                return itemAbility;
            }
        }
        return null;
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