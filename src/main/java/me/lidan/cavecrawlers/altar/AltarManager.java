package me.lidan.cavecrawlers.altar;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AltarManager {
    private static AltarManager instance;
    private Map<String, Altar> altars = new HashMap<>();

    public void registerAltar(String name, Altar altar) {
        altar.setId(name);
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

    public void updateAltar(String name, Altar altar) {
        altars.put(name, altar);
        AltarLoader.getInstance().update(name, altar);
    }

    public List<String> getAltarNames() {
        return new ArrayList<>(altars.keySet());
    }

    public void reset() {
        for (Altar altar : altars.values()) {
            altar.resetAltar();
        }
    }

    public static AltarManager getInstance() {
        if (instance == null) {
            instance = new AltarManager();
        }
        return instance;
    }
}
