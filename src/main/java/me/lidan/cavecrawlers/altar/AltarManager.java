package me.lidan.cavecrawlers.altar;

import me.lidan.cavecrawlers.items.ItemInfo;
import org.bukkit.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AltarManager {
    private static final Logger log = LoggerFactory.getLogger(AltarManager.class);
    private static AltarManager instance;
    private Map<String, Altar> altars = new HashMap<>();

    public void registerAltar(String name, Altar altar) {
        altar.setId(name);
        altars.put(name, altar);
    }

    public Altar getAltar(String name) {
        return altars.get(name);
    }

    public Altar getAltarAtLocation(Location location, ItemInfo itemInfo) {
        for (Altar altar : altars.values()) {
            if (altar.isAltar(location) && altar.getItemToSpawn().equals(itemInfo)) {
                return altar;
            }
        }
        return null;
    }

    public List<Altar> getAltarsWithMob(String mobName) {
        List<Altar> result = new ArrayList<>();
        for (Altar altar : altars.values()) {
            AltarDrop foundDrop = altar.getDropByMobName(mobName);
            if (foundDrop != null) {
                result.add(altar);
            }
        }
        return result;
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
