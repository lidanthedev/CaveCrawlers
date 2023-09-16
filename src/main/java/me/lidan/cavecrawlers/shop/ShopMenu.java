package me.lidan.cavecrawlers.shop;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopMenu implements ConfigurationSerializable {
    private final String title;
    private final List<ShopItem> shopItemList;
    private final Gui gui;

    public ShopMenu(String title, List<ShopItem> shopItemList) {
        this.title = title;
        this.shopItemList = shopItemList;
        this.gui = Gui.gui().title(Component.text(this.title)).rows(6).disableAllInteractions().create();
        gui.getFiller().fillBorder(ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Component.text("")).asGuiItem());
        for (ShopItem shopItem : this.shopItemList) {
            GuiItem guiItem = ItemBuilder.from(shopItem.toItem()).asGuiItem(event -> {
                if (event.getWhoClicked() instanceof Player player) {
                    boolean buy = shopItem.buy(player);
                    if (!buy) {
                        player.sendMessage(ChatColor.RED + "You don't have the items!");
                    }
                }
            });
            gui.addItem(guiItem);
        }
    }

    public void open(Player player){
        gui.open(player);
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        List<Map<String, Object>> itemList = new ArrayList<>();
        for (ShopItem shopItem : shopItemList) {
            itemList.add(shopItem.serialize());
        }
        map.put("items", itemList);
        return map;
    }

    public static ShopMenu deserialize(Map<String, Object> map) {
        String title = (String) map.get("title");

        List<ShopItem> items = new ArrayList<>();
        List<Map<String, Object>> itemList = (List<Map<String, Object>>) map.get("items");
        for (Map<String, Object> itemMap : itemList) {
            ShopItem item = ShopItem.deserialize(itemMap);
            items.add(item);
        }

        return new ShopMenu(title, items);
    }

}
