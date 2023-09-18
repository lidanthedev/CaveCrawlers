package me.lidan.cavecrawlers.shop;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.utils.CustomConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class ShopManager {
    private static ShopManager instance;

    private final Map<String, ShopMenu> menuMap = new HashMap<>();

    public void registerMenu(String key, ShopMenu menu) {
        menuMap.put(key, menu);
    }

    public @Nullable ShopMenu getShop(String ID){
        return menuMap.get(ID);
    }

    public CustomConfig getConfig(String ID){
        ShopLoader shopLoader = ShopLoader.getInstance();
        Map<String, File> idFileMap = shopLoader.getItemIDFileMap();
        File file = idFileMap.get(ID);
        if (file == null){
            file = new File(shopLoader.getFileDir(), ID + ".yml");
        }
        return new CustomConfig(file);
    }

    public ShopMenu createShop(String shopID){
        String title = shopID;
        title = title.replaceAll("_"," ");

        ShopMenu shopMenu = new ShopMenu(title, new ArrayList<>());
        CustomConfig config = getConfig(shopID);
        config.set(shopID, shopMenu);
        config.save();
        registerMenu(shopID, shopMenu);
        return shopMenu;
    }

    public void addItemToShop(String shopID, String resultID, String ingredientID, int amount) {
        ShopItem shopItem = new ShopItem(resultID, ingredientID, amount);
        addItemToShop(shopID, shopItem);
    }

    public void addItemToShop(String shopID, ShopItem shopItem) {
        ShopMenu shopMenu = getShop(shopID);
        List<ShopItem> shopItemList = shopMenu.getShopItemList();
        shopItemList.add(shopItem);
        shopMenu.buildGui();

        CustomConfig shopConfig = getConfig(shopID);
        shopConfig.set(shopID, shopMenu);
        shopConfig.save();
    }

    public Set<String> getKeys() {
        return menuMap.keySet();
    }

    public void clear(){
        menuMap.clear();
    }

    public static ShopManager getInstance() {
        if (instance == null){
            instance = new ShopManager();
        }
        return instance;
    }
}
