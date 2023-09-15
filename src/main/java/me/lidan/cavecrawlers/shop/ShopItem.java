package me.lidan.cavecrawlers.shop;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import lombok.Data;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.utils.StringUtils;
import me.lidan.cavecrawlers.utils.VaultUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
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
    private ItemsManager itemsManager;

    public ShopItem(ItemInfo result, int resultAmount, double price, Map<ItemInfo, Integer> itemsMap) {
        this.result = result;
        this.resultAmount = resultAmount;
        this.price = price;
        this.itemsMap = itemsMap;
        itemsManager = ItemsManager.getInstance();
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
}
