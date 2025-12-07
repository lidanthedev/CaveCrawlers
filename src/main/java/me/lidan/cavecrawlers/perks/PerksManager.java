package me.lidan.cavecrawlers.perks;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class PerksManager {
    private static PerksManager instance;
    private final Map<String, Perk> perks = new HashMap<>();

    private PerksManager() {

    }

    public void register(String key, Perk value) {
        perks.put(key, value);
    }

    public Map<String, Perk> getPerks(Player player) {
        Map<String, Perk> playerPerks = new HashMap<>();
        for (String key : perks.keySet()) {
            Perk perk = perks.get(key);
            if (player.hasPermission(perk.getPermission())) {
                if (playerPerks.containsKey(perk.getTrack())) {
                    Perk currentPerk = playerPerks.get(perk.getTrack());
                    if (perk.getPriority() > currentPerk.getPriority()) {
                        playerPerks.put(perk.getTrack(), perk);
                    }
                } else {
                    playerPerks.put(perk.getTrack(), perk);
                }
            }
        }
        return playerPerks;
    }

    public static PerksManager getInstance() {
        if (instance == null) {
            instance = new PerksManager();
        }
        return instance;
    }
}
