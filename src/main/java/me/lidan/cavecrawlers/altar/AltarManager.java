package me.lidan.cavecrawlers.altar;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;

public class AltarManager {
    private static AltarManager instance;
    private Map<String, Altar> altars = new HashMap<>();

    public void registerAltar(String name, Altar altar) {
        altars.put(name, altar);
    }

    public Altar getAltar(String name) {
        return altars.get(name);
    }

    public Altar getAltarAtLocation(Location location) {
        for (Altar altar : altars.values()) {
            if (altar.isAltar(location)) {
                return altar;
            }
        }
        return null;
    }

    public static AltarManager getInstance() {
        if (instance == null) {
            instance = new AltarManager();
        }
        return instance;
    }
}
