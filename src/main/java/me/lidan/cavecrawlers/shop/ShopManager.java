package me.lidan.cavecrawlers.shop;

import me.lidan.cavecrawlers.CaveCrawlers;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ShopManager {
    private static ShopManager instance;

    private final Map<String, ShopMenu> menuMap = new HashMap<>();

    public void registerMenu(String key, ShopMenu menu) {
        menuMap.put(key, menu);
        CaveCrawlers.getInstance().getLogger().info("Loaded Shop " + key);
    }

    public @Nullable ShopMenu getShop(String ID){
        return menuMap.get(ID);
    }

    public Set<String> getKeys() {
        return menuMap.keySet();
    }

    public static ShopManager getInstance() {
        if (instance == null){
            instance = new ShopManager();
        }
        return instance;
    }
}
