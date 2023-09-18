package me.lidan.cavecrawlers.shop;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import lombok.Data;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.utils.StringUtils;
import me.lidan.cavecrawlers.utils.VaultUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ShopItem implements ConfigurationSerializable {
    private final ItemInfo result;
    private final int resultAmount;
    private final double price;
    private final Map<ItemInfo, Integer> itemsMap;
    private ItemsManager itemsManager;

    public ShopItem(ItemInfo result, int resultAmount, double price, Map<ItemInfo, Integer> itemsMap) {
        this.result = result;
        this.resultAmount = resultAmount;
        this.price = price;
        this.itemsMap = itemsMap;
        itemsManager = ItemsManager.getInstance();
    }

    public ShopItem(ItemInfo result, ItemInfo ingredient, int amount){
        this(result, 1, 0, Map.of(ingredient, amount));
    }

    public ShopItem(String resultID, String ingredientID, int amount){
        this(getItemByID(resultID), getItemByID(ingredientID), amount);
    }

    public List<String> toList(){
        List<String> list = new ArrayList<>(result.toList());

        list.set(0, formatName(list.get(0), resultAmount));

        list.add("");
        list.add(ChatColor.GRAY + "Cost");
        if (price > 0){
            list.add(ChatColor.GOLD + StringUtils.getNumberFormat(price) + " Coins");
        }
        for (ItemInfo itemInfo : itemsMap.keySet()) {
            int amount = itemsMap.get(itemInfo);
            String name = itemInfo.getName();
            list.add(formatName(name, amount));
        }
        list.add("");
        list.add(ChatColor.YELLOW + "Click to Trade");
        return list;
    }

    public ItemStack toItem(){
        ItemStack itemStack = itemsManager.buildItem(result, resultAmount);
        List<String> list = toList();
        String name = list.get(0);
        list.remove(0);
        return ItemBuilder.from(itemStack).setName(name).setLore(list).build();
    }

    @NotNull
    private String formatName(String name, int amount) {
        name += " " + ChatColor.DARK_GRAY + "x" + amount;
        return name;
    }

    public boolean buy(Player player) {
        if (VaultUtils.getCoins(player) > price){
            if (itemsManager.hasItems(player, itemsMap)){
                VaultUtils.takeCoins(player, price);
                itemsManager.removeItems(player, itemsMap);
                ItemStack itemStack = itemsManager.buildItem(result, resultAmount);
                player.getInventory().addItem(itemStack);
                return true;
            }
        }
        return false;
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("result", result.getID());
        map.put("resultAmount", resultAmount);
        map.put("price", price);

        Map<String, Integer> itemIDmap = new HashMap<>();
        for (ItemInfo itemInfo : itemsMap.keySet()) {
            int amount = itemsMap.get(itemInfo);
            itemIDmap.put(itemInfo.getID(), amount);
        }
        map.put("item-cost",itemIDmap);
        return map;
    }

    public static ShopItem deserialize(Map<String, Object> map){
        String resultId = (String) map.get("result");
        int resultAmount = (int) map.get("resultAmount");

        ItemInfo result = getItemByID(resultId);

        double price = (double) map.get("price");

        Map<String, Integer> itemIdMap = (Map<String, Integer>) map.get("item-cost");
        Map<ItemInfo, Integer> itemsMap = new HashMap<>();

        for (String itemId : itemIdMap.keySet()) {
            ItemInfo itemInfo = getItemByID(itemId);
            int amount = itemIdMap.get(itemId);
            itemsMap.put(itemInfo, amount);
        }

        return new ShopItem(result, resultAmount, price, itemsMap);
    }

    @Nullable
    private static ItemInfo getItemByID(String resultID) {
        return ItemsManager.getInstance().getItemByID(resultID);
    }
}
