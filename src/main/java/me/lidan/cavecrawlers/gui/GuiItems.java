package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GuiItems {
    public static final ItemBuilder BACK_ITEM = ItemBuilder.from(Material.ARROW).setName(ChatColor.GRAY + "Go Back").setLore("", "§eClick to To Back");
    public static final ItemBuilder NEXT_ARROW_ITEM = ItemBuilder.from(Material.ARROW).setName(net.md_5.bungee.api.ChatColor.BLUE + "Next");
    public static final ItemBuilder PREVIOUS_ARROW_ITEM = ItemBuilder.from(Material.ARROW).setName(net.md_5.bungee.api.ChatColor.BLUE + "Previous");

    public static final @NotNull GuiItem CLOSE_ITEM = ItemBuilder.from(Material.BARRIER).setName(ChatColor.RED + "Close Menu").setLore("", "§eClick to Close Menu").asGuiItem((event -> {
        Player sender = (Player) event.getWhoClicked();
        sender.closeInventory();
    }));

    public static final @NotNull GuiItem BACK_MENU_ITEM = BACK_ITEM.asGuiItem((event -> {
        Player sender = (Player) event.getWhoClicked();
        MenuGui menuGui = new MenuGui(sender);
        menuGui.open();
    }));
    public static final @NotNull GuiItem GLASS_ITEM = ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Component.text("")).asGuiItem();

    public static void setupNextPreviousItems(PaginatedGui gui, int row) {
        setupNextPreviousItems(gui, row, 1, 9);
    }

    public static void setupNextPreviousItems(PaginatedGui gui, int row, int prevColumn, int nextColumn) {
        gui.setItem(row, nextColumn, GuiItems.GLASS_ITEM);
        gui.setItem(row, prevColumn, GuiItems.GLASS_ITEM);
        if (gui.getNextPageNum() != gui.getCurrentPageNum()) {
            gui.setItem(row, nextColumn, GuiItems.NEXT_ARROW_ITEM.asGuiItem(event -> {
                gui.next();
                setupNextPreviousItems(gui, row, prevColumn, nextColumn);
            }));
        }
        if (gui.getPrevPageNum() != gui.getCurrentPageNum()) {
            gui.setItem(row, prevColumn, GuiItems.PREVIOUS_ARROW_ITEM.asGuiItem(event -> {
                gui.previous();
                setupNextPreviousItems(gui, row, prevColumn, nextColumn);
            }));
        }
        gui.update();
    }
}
