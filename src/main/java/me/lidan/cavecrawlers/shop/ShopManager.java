package me.lidan.cavecrawlers.shop;

import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.utils.CustomConfig;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ShopManager {
    private static ShopManager instance;

    private final Map<String, ShopMenu> menuMap = new HashMap<>();

    public void registerMenu(String key, ShopMenu menu) {
        menu.setId(key);
        menuMap.put(key, menu);
    }

    public @Nullable ShopMenu getShop(String ID){
        return menuMap.get(ID);
    }

    public CustomConfig getConfig(String ID){
        ShopLoader shopLoader = ShopLoader.getInstance();
        return shopLoader.getConfig(ID);
    }

    public ShopMenu createShop(String shopID){
        String title = shopID;
        title = title.replaceAll("_"," ");

        ShopMenu shopMenu = new ShopMenu(title, new ArrayList<>());
        shopMenu.setId(shopID);
        saveShop(shopID, shopMenu);
        registerMenu(shopID, shopMenu);
        return shopMenu;
    }

    public void addItemToShop(String shopID, ItemStack resultItem, ItemStack ingredient) {
        addItemToShop(shopID, resultItem, new ItemStack[] {ingredient});
    }

    public void addItemToShop(String shopID, ItemStack resultItem, ItemStack... ingredients) {
        ItemsManager itemsManager = ItemsManager.getInstance();
        ItemInfo resultID = itemsManager.getItemFromItemStackSafe(resultItem);

        Map<ItemInfo, Integer> ingredientsMap = new HashMap<>();
        for (ItemStack itemStack : ingredients) {
            if (itemStack != null) {
                ingredientsMap.put(itemsManager.getItemFromItemStackSafe(itemStack), itemStack.getAmount());
            }
        }

        ShopItem shopItem = new ShopItem(resultID, resultItem.getAmount(), 0, ingredientsMap);
        addItemToShop(shopID, shopItem);
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

        saveShop(shopID, shopMenu);
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

    public void updateShop(String shopID, int slotID, String ingredientID, int amount) {
        ShopMenu shopMenu = getShop(shopID);
        assert shopMenu != null;
        ShopItem shopItem = shopMenu.getShopItemList().get(slotID);
        ItemInfo itemInfo = ItemsManager.getInstance().getItemByID(ingredientID);
        updateShop(shopMenu, shopItem, itemInfo, amount);
    }

    public void updateShop(ShopMenu shopMenu, ShopItem shopItem, ItemInfo itemInfo, int amount) {
        Map<ItemInfo, Integer> itemsMap = shopItem.getItemsMap();
        if (amount == 0){
            itemsMap.remove(itemInfo);
        }
        else {
            itemsMap.put(itemInfo, amount);
        }
        shopMenu.buildGui();

        saveShop(shopMenu.getId(), shopMenu);
    }

    public void updateShopCoins(String shopID, int slotID, double coins) {
        ShopMenu shopMenu = getShop(shopID);
        assert shopMenu != null;
        ShopItem shopItem = shopMenu.getShopItemList().get(slotID);
        shopItem.setPrice(coins);
        shopMenu.buildGui();

        saveShop(shopID, shopMenu);
    }

    public void saveShop(String shopID, ShopMenu shopMenu) {
        CustomConfig shopConfig = getConfig(shopID);
        shopConfig.set(shopID, shopMenu);
        shopConfig.save();
    }

    public void deleteShop(String shopID) {
        ShopMenu shopMenu = getShop(shopID);
        assert shopMenu != null;
        menuMap.remove(shopID);

        saveShop(shopID, null);
    }

    public void removeShop(String shopID, int slotID) {
        ShopMenu shopMenu = getShop(shopID);
        assert shopMenu != null;
        removeShopItem(shopMenu, slotID);
    }

    public void removeShopItem(ShopMenu shopMenu, int slotID) {
        String shopID = shopMenu.getId();
        List<ShopItem> shopItemList = shopMenu.getShopItemList();
        shopItemList.remove(slotID);
        shopMenu.buildGui();

        saveShop(shopID, shopMenu);
    }
}
