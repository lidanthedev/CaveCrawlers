package me.lidan.cavecrawlers.shop;

import lombok.Data;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ShopItem {
    private final ItemInfo result;
    private final int resultAmount;
    private final double price;
    private final Map<ItemInfo, Integer> itemsMap;

    public ShopItem(ItemInfo result, int resultAmount, double price, Map<ItemInfo, Integer> itemsMap) {
        this.result = result;
        this.resultAmount = resultAmount;
        this.price = price;
        this.itemsMap = itemsMap;
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

    @NotNull
    private String formatName(String name, int amount) {
        name += " " + ChatColor.DARK_GRAY + "x" + amount;
        return name;
    }
}
