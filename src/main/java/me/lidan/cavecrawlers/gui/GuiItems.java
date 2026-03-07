package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import me.lidan.cavecrawlers.commands.MenuCommands;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GuiItems {
    public static final ItemBuilder BACK_ITEM = ItemBuilder.from(Material.ARROW).name(MiniMessageUtils.miniMessage("<gray>Go Back")).lore(MiniMessageUtils.miniMessageList("", "<yellow>Click to go Back"));
    public static final ItemBuilder NEXT_ARROW_ITEM = ItemBuilder.from(Material.ARROW).name(MiniMessageUtils.miniMessage("<blue>Next")).lore(MiniMessageUtils.miniMessageList("", "<yellow>Click to go Next", "<gold>Right-click to go to last page"));
    public static final ItemBuilder PREVIOUS_ARROW_ITEM = ItemBuilder.from(Material.ARROW).name(MiniMessageUtils.miniMessage("<blue>Previous")).lore(MiniMessageUtils.miniMessageList("", "<yellow>Click to go Previous", "<gold>Right-click to go to first page"));

    public static final @NotNull GuiItem CLOSE_ITEM = ItemBuilder.from(Material.BARRIER).name(MiniMessageUtils.miniMessage("<red>Close Menu")).lore(MiniMessageUtils.miniMessageList("", "<yellow>Click to Close Menu")).asGuiItem((event -> {
        Player sender = (Player) event.getWhoClicked();
        sender.closeInventory();
    }));

    public static final @NotNull GuiItem BACK_MENU_ITEM = BACK_ITEM.asGuiItem((event -> {
        Player sender = (Player) event.getWhoClicked();
        MenuCommands.showMenu(sender);
    }));
    public static final @NotNull GuiItem GLASS_ITEM = ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Component.text("")).asGuiItem();

    public static void setupNextPreviousItems(PaginatedGui gui, int row) {
        setupNextPreviousItems(gui, row, 1, 9);
    }

    public static void setupNextPreviousItems(PaginatedGui gui, int row, int prevColumn, int nextColumn) {
        setupNextPreviousItems(gui, row, row, prevColumn, nextColumn);
    }

    public static void setupNextPreviousItems(PaginatedGui gui, int prevRow, int nextRow, int prevColumn, int nextColumn) {
        gui.setItem(nextRow, nextColumn, GuiItems.GLASS_ITEM);
        gui.setItem(prevRow, prevColumn, GuiItems.GLASS_ITEM);
        if (gui.getNextPageNum() != gui.getCurrentPageNum()) {
            gui.setItem(nextRow, nextColumn, GuiItems.NEXT_ARROW_ITEM.asGuiItem(event -> {
                if (event.isRightClick()) {
                    gui.setPageNum(gui.getPagesNum());
                } else {
                    gui.next();
                }
                setupNextPreviousItems(gui, prevRow, nextRow, prevColumn, nextColumn);
            }));
        }
        if (gui.getPrevPageNum() != gui.getCurrentPageNum()) {
            gui.setItem(prevRow, prevColumn, GuiItems.PREVIOUS_ARROW_ITEM.asGuiItem(event -> {
                if (event.isRightClick()) {
                    gui.setPageNum(1);
                } else {
                    gui.previous();
                }
                setupNextPreviousItems(gui, prevRow, nextRow, prevColumn, nextColumn);
            }));
        }
        gui.update();
    }

    public static List<Integer> getLayoutForItems(int n) {
        return switch (n) {
            case 0 -> List.of();
            case 1 -> List.of(5);
            case 2 -> List.of(4, 6);
            case 3 -> List.of(4, 5, 6);
            case 4 -> List.of(3, 4, 6, 7);
            case 5 -> List.of(3, 4, 5, 6, 7);
            case 6 -> List.of(2, 3, 4, 6, 7, 8);
            default -> // 7 or more
                    List.of(2, 3, 4, 5, 6, 7, 8);
        };
    }
}
