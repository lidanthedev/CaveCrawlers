package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.lidan.cavecrawlers.gui.GuiItems;
import me.lidan.cavecrawlers.gui.PaginatedRowGui;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class IndexMainMenu extends PaginatedRowGui {
    private final Player player;

    public IndexMainMenu(Player player) {
        super(Gui.gui().rows(6).title(MiniMessageUtils.miniMessage("Index Main Menu")).create());
        this.player = player;
        gui.disableAllInteractions();
        gui.getFiller().fill(GuiItems.GLASS_ITEM);

        for (IndexCategory category : IndexCategory.values()) {
            GuiItem guiItem = category.getGuiItem();
            items.add(guiItem);
        }

        gui.setItem(6, 5, GuiItems.CLOSE_ITEM);
        if (player.hasPermission(IndexBaseCategoryMenu.CAVECRAWLERS_INDEX_ADMIN_PERMISSION)) {
            gui.setItem(6, 3, ItemBuilder.from(Material.REDSTONE).name(MiniMessageUtils.miniMessage("<red>Hidden Values")).asGuiItem(event -> new IndexHiddenMenu(player).open()));
        }
        updateItems();
    }

    public void open() {
        gui.open(player);
    }
}
