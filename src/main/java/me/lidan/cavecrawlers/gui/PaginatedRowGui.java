package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public abstract class PaginatedRowGui {
    protected final Gui gui;
    protected final List<GuiItem> items = new ArrayList<>();
    protected int currentPage = 0;

    protected PaginatedRowGui(Gui gui) {
        this.gui = gui;
    }


    public void updateItems() {
        List<GuiItem> guiItems = items.subList(currentPage * 7, Math.min(7 * (currentPage + 1), items.size()));
        int size = guiItems.size();
        List<Integer> layoutForItems = getLayoutForItems(size);
        getLayoutForItems(7).forEach(i -> gui.setItem(getRow(), i, GuiItems.GLASS_ITEM));
        for (int i = 0; i < size; i++) {
            gui.setItem(getRow(), layoutForItems.get(i), guiItems.get(i));
        }


        if (items.size() > 7) {
            gui.setItem(getRow(), 1, ItemBuilder.from(Material.ARROW).setName(ChatColor.BLUE + "Previous").asGuiItem(event -> previous()));
            gui.setItem(getRow(), 9, ItemBuilder.from(Material.ARROW).setName(ChatColor.BLUE + "Next").asGuiItem(event -> next()));
        }
        gui.update();
    }

    /**
     * Override to change the row of the gui
     *
     * @return the row of the gui
     */
    protected int getRow() {
        return 3;
    }

    public void next() {
        if (currentPage * 7 + 7 >= items.size()) {
            return;
        }
        currentPage++;
        updateItems();
    }

    public void previous() {
        if (currentPage == 0) {
            return;
        }
        currentPage--;
        updateItems();
    }

    public List<Integer> getLayoutForItems(int n) {
        return switch (n) {
            case 1 -> List.of(5);
            case 2 -> List.of(4, 6);
            case 3 -> List.of(4, 5, 6);
            case 4 -> List.of(2, 4, 6, 8);
            case 5 -> List.of(3, 4, 5, 6, 7);
            case 6 -> List.of(2, 3, 4, 6, 7, 8);
            default -> // 7 or more
                    List.of(2, 3, 4, 5, 6, 7, 8);
        };
    }
}
