package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.PaginatedGui;
import me.lidan.cavecrawlers.gui.GuiItems;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class IndexHiddenMenu {
    private final Player player;
    private final PaginatedGui gui;

    public IndexHiddenMenu(Player player) {
        this.player = player;
        this.gui = Gui.paginated().pageSize(28).rows(6).title(MiniMessageUtils.miniMessage("Hidden Entries")).create();
        gui.disableAllInteractions();
        gui.getFiller().fillBorder(GuiItems.GLASS_ITEM);
        gui.setItem(6, 5, GuiItems.BACK_ITEM.asGuiItem(event -> new IndexMainMenu(player).open()));
        IndexManager indexManager = IndexManager.getInstance();
        for (String entryName : indexManager.getAllHiddenEntries()) {
            gui.addItem(ItemBuilder.from(Material.BOOK).name(MiniMessageUtils.miniMessage("<gray>" + entryName)).lore(MiniMessageUtils.miniMessageList("<dark_gray>Hidden Entry", "", "<yellow>Click to unhide")).asGuiItem(event -> {
                player.sendMessage("unhiding entry: " + entryName);
                indexManager.toggleHiddenEntry(entryName);
                new IndexHiddenMenu(player).open();
            }));
        }
        for (String entryName : indexManager.getAllHiddenDrops()) {
            gui.addItem(ItemBuilder.from(Material.PAPER).name(MiniMessageUtils.miniMessage("<gray>" + entryName)).lore(MiniMessageUtils.miniMessageList("<dark_gray>Hidden Drop", "", "<yellow>Click to unhide")).asGuiItem(event -> {
                player.sendMessage("Unhiding drop: " + entryName);
                indexManager.setHiddenDrop(IndexManager.HIDDEN_DROPS_KEY, entryName, false);
                new IndexHiddenMenu(player).open();
            }));
        }
    }

    public void open() {
        gui.open(player);
    }
}
