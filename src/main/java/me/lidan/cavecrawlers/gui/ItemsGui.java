package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.GuiItem;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public class ItemsGui extends PaginatedSelector<ItemInfo> {
    private static final ItemsManager itemsManager = ItemsManager.getInstance();

    private static final BiConsumer<InventoryClickEvent, ItemInfo> DEFAULT_CALLBACK = (event, itemInfo) -> {
        HumanEntity clicked = event.getWhoClicked();
        ItemStack itemStack = itemsManager.buildItem(itemInfo, 1);
        clicked.getInventory().addItem(itemStack);
    };

    public ItemsGui(Player player, String query, BiConsumer<InventoryClickEvent, ItemInfo> callback, Component title) {
        super(player, query, title, callback);
    }

    public ItemsGui(Player player, String query) {
        this(player, query, DEFAULT_CALLBACK, MiniMessageUtils.miniMessage("<blue>Items Browser"));
    }

    @Override
    public void setupGui() {
        Set<String> keys = itemsManager.getKeys();
        List<String> sortedKeys = new ArrayList<>(keys);
        Collections.sort(sortedKeys);
        for (String ID : sortedKeys) {
            ItemInfo itemInfo = itemsManager.getItemByID(ID);
            if (itemInfo == null) {
                continue;
            }
            ItemStack itemStack = itemsManager.buildItem(itemInfo, 1);
            if ((itemStack.hasItemMeta() && itemStack.getItemMeta().getDisplayName().toLowerCase().contains(query)) || ID.toLowerCase().contains(query)) {
                GuiItem guiItem = ItemBuilder.from(itemStack.clone()).addLore(ChatColor.DARK_GRAY + "ID: " + ID).asGuiItem(event -> {
                    callback.accept(event, itemInfo);
                });
                gui.addItem(guiItem);
            }
        }
    }

    @Override
    protected void searchInternal(String query) {
        new ItemsGui(player, query, callback, title).open();
    }
}
