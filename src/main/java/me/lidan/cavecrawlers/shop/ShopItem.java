package me.lidan.cavecrawlers.shop;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import lombok.Data;
import lombok.NonNull;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import me.lidan.cavecrawlers.utils.StringUtils;
import me.lidan.cavecrawlers.utils.VaultUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ShopItem implements ConfigurationSerializable {
    private static final Logger log = LoggerFactory.getLogger(ShopItem.class);
    private final ConfigMessage BUY_ITEM_MESSAGE = ConfigMessage.getMessageOrDefault("buy_item", "&eYou bought %formatted_name%&e for &6%price% coins");
    private ItemInfo result;
    private int resultAmount;
    private double price;
    private Map<ItemInfo, Integer> ingredientsMap;
    private ItemsManager itemsManager;

    public ShopItem(@NonNull ItemInfo result, int resultAmount, double price, Map<ItemInfo, Integer> ingredientsMap) {
        this.result = result;
        this.resultAmount = resultAmount;
        this.price = price;
        this.ingredientsMap = ingredientsMap;
        itemsManager = ItemsManager.getInstance();
    }

    public ShopItem(ItemInfo result, ItemInfo ingredient, int amount){
        this(result, 1, 0, Map.of(ingredient, amount));
    }

    public ShopItem(String resultID, String ingredientID, int amount){
        this(getItemByID(resultID), getItemByID(ingredientID), amount);
    }

    @NonNull
    public static String formatName(String name, int amount) {
        name += " " + ChatColor.DARK_GRAY + "x" + amount;
        return name;
    }

    public ItemStack toItem(){
        ItemStack itemStack = itemsManager.buildItem(result, resultAmount);
        List<String> list = toList();
        String name = list.get(0);
        list.remove(0);
        return ItemBuilder.from(itemStack).setName(name).setLore(list).build();
    }

    public List<String> toList(){
        List<String> list = new ArrayList<>(result.toList());

        list.set(0, formatName(list.get(0), resultAmount));

        list.add("");
        list.add(ChatColor.GRAY + "Cost");
        double normalizedPrice = normalizedPrice();
        if (price == 0 && ingredientsMap.isEmpty()) {
            list.add(ChatColor.GOLD + "Free");
        } else {
            if (normalizedPrice > 0 && isChargeable(price, normalizedPrice)) {
                list.add(ChatColor.GOLD + StringUtils.getNumberFormat(normalizedPrice) + " Coins");
            }
            for (ItemInfo itemInfo : ingredientsMap.keySet()) {
                if (itemInfo == null) {
                    log.warn("ShopItem has null ingredient itemInfo for result: {}", result.getID());
                    continue;
                }
                int amount = ingredientsMap.get(itemInfo);
                String name = itemInfo.getFormattedName();
                list.add(formatName(name, amount));
            }
        }
        list.add("");
        list.add(ChatColor.YELLOW + "Click to Trade");
        return list;
    }

    public boolean buy(Player player) {
        return buy(player, false);
    }

    public boolean buy(Player player, boolean silent) {
        ShopPurchaseEvent event = new ShopPurchaseEvent(player, this, price, ingredientsMap, resultAmount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        double price = event.getPrice();
        double normalizedPrice = normalizedPrice(price);
        Map<ItemInfo, Integer> ingredients = event.getIngredients();
        int resultAmount = event.getResultAmount();
        if (canBuy(player, price, ingredients, resultAmount)){
            if (normalizedPrice > 0 && !VaultUtils.takeCoins(player, normalizedPrice)) {
                return false;
            }
            itemsManager.removeItems(player, ingredients);
            itemsManager.giveItem(player, result, resultAmount);
            Map<String, String> placeholders = Map.of(
                    "item", result.getFormattedName(),
                    "amount", String.valueOf(resultAmount),
                    "price", StringUtils.getNumberFormat(normalizedPrice),
                    "formatted_name", formatName(result.getFormattedName(), resultAmount)
            );
            if (!silent) {
                BUY_ITEM_MESSAGE.sendMessage(player, placeholders);
            }
            return true;
        }
        return false;
    }

    public boolean canBuy(Player player) {
        return canBuy(player, price, ingredientsMap, resultAmount);
    }

    private double normalizedPrice() {
        return normalizedPrice(price);
    }

    private double normalizedPrice(double price) {
        return Math.floor(price * 10d) / 10d;
    }

    private boolean canBuy(Player player, double price, Map<ItemInfo, Integer> ingredients, int resultAmount) {
        double normalizedPrice = normalizedPrice(price);
        return resultAmount > 0 && isChargeable(price, normalizedPrice)
                && hasValidIngredients(ingredients)
                && VaultUtils.getCoins(player) >= normalizedPrice && itemsManager.hasItems(player, ingredients);
    }

    private boolean isChargeable(double price, double normalizedPrice) {
        return Double.isFinite(price) && price >= 0 && Double.isFinite(normalizedPrice)
                && (price == 0 || normalizedPrice > 0);
    }

    private boolean hasValidIngredients(Map<ItemInfo, Integer> ingredients) {
        for (Map.Entry<ItemInfo, Integer> entry : ingredients.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() < 0) {
                return false;
            }
        }
        return true;
    }

    @NonNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("result", result.getID());
        map.put("resultAmount", resultAmount);
        map.put("price", price);
        map.put("item-cost", itemsManager.itemMapToStringMap(ingredientsMap));
        return map;
    }

    public static ShopItem deserialize(Map<String, Object> map){
        String resultId = (String) map.get("result");
        int resultAmount = (int) map.get("resultAmount");

        ItemInfo result = getItemByID(resultId);

        double price = (double) map.get("price");

        Map<String, Integer> itemIdMap = (Map<String, Integer>) map.getOrDefault("item-cost", new HashMap<>());
        Map<ItemInfo, Integer> itemsMap = ItemsManager.getInstance().stringMapToItemMap(itemIdMap);

        return new ShopItem(result, resultAmount, price, itemsMap);
    }

    @Nullable
    private static ItemInfo getItemByID(String resultID) {
        return ItemsManager.getInstance().getItemByID(resultID);
    }
}
