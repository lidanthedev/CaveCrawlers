package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.prompt.PromptManager;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ItemsGui  {
    private final Player player;
    private final PaginatedGui gui;
    private static final ItemsManager itemsManager = ItemsManager.getInstance();

    private static final ItemsGuiCallback DEFAULT_CALLBACK = (event, itemInfo) -> {
        HumanEntity clicked = event.getWhoClicked();
        ItemStack itemStack = itemsManager.buildItem(itemInfo, 1);
        clicked.getInventory().addItem(itemStack);
    };

    public ItemsGui(Player player, String query) {
        this(player, query, DEFAULT_CALLBACK, MiniMessageUtils.miniMessage("<blue>Items Browser"));
    }

    public ItemsGui(Player player, String query, ItemsGuiCallback callback, Component title) {
        this.player = player;
        this.gui = Gui.paginated()
                .title(title)
                .rows(6)
                .pageSize(45) // Set the size you want, or leave it to be automatic.
                .create();
        query = query.toLowerCase();
        gui.disableAllInteractions();
        // filler
        gui.getFiller().fillBottom(GuiItems.GLASS_ITEM);
        // Previous item
        gui.setItem(6, 3, ItemBuilder.from(Material.ARROW).setName(ChatColor.BLUE + "Previous").asGuiItem(event -> gui.previous()));
        // Next item
        gui.setItem(6, 7, ItemBuilder.from(Material.ARROW).setName(ChatColor.BLUE + "Next").asGuiItem(event -> gui.next()));
        // Search
        gui.setItem(6, 5, ItemBuilder.from(Material.COMPASS).name(MiniMessageUtils.miniMessage("<blue>Search")).asGuiItem(event -> {
            PromptManager.getInstance().prompt(player, "Search").thenAccept(result -> {
                new ItemsGui(player, result, callback, title).open();
            }).exceptionally(throwable -> {
                new ItemsGui(player, "", callback, title).open();
                return null;
            });
        }));

        Set<String> keys = itemsManager.getKeys();
        List<String> sortedKeys = new ArrayList<>(keys);
        Collections.sort(sortedKeys);
        for (String ID : sortedKeys) {
            ItemStack itemStack = itemsManager.buildItem(ID, 1);
            if ((itemStack.hasItemMeta() && itemStack.getItemMeta().getDisplayName().toLowerCase().contains(query)) || ID.toLowerCase().contains(query)) {
                GuiItem guiItem = ItemBuilder.from(itemStack.clone()).addLore(ChatColor.DARK_GRAY + "ID: " + ID).asGuiItem(event -> {
                    callback.callback(event, itemsManager.getItemByID(ID));
                });
                gui.addItem(guiItem);
            }
        }
    }

    public void open(){
        gui.open(player);
    }
}
