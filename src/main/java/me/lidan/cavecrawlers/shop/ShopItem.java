package me.lidan.cavecrawlers.shop;

import lombok.Data;
import me.lidan.cavecrawlers.items.ItemInfo;

import java.util.Arrays;
import java.util.List;

@Data
public class ShopItem {
    private final ItemInfo result;
    private final double price;
    private final List<ItemInfo> items;

    public ShopItem(ItemInfo result, double price, List<ItemInfo> items) {
        this.result = result;
        this.price = price;
        this.items = items;
    }

    public ShopItem(ItemInfo result, double price, ItemInfo... items) {
        this(result, price, Arrays.asList(items));
    }
}
