package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.PaginatedGui;
import me.lidan.cavecrawlers.prompt.PromptManager;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.function.BiConsumer;

public abstract class PaginatedSelector<T> {
    public static ItemBuilder SEARCH_ITEM = ItemBuilder.from(Material.COMPASS).name(Component.text("<blue>Search")).lore(MiniMessageUtils.miniMessageList("", "<yellow>Click to search", "<red>Right click to reset search"));
    protected final Player player;
    protected final String query;
    protected final Component title;
    protected final BiConsumer<InventoryClickEvent, T> callback;
    protected PaginatedGui gui;

    public PaginatedSelector(Player player, String query, Component title, BiConsumer<InventoryClickEvent, T> callback) {
        this.player = player;
        this.title = title;
        this.callback = callback;
        this.gui = Gui.paginated()
                .title(title)
                .rows(6)
                .pageSize(28)
                .create();
        this.query = query.toLowerCase();
        gui.disableAllInteractions();
        gui.getFiller().fillBorder(GuiItems.GLASS_ITEM);
        gui.setItem(6, 5, GuiItems.BACK_ITEM.asGuiItem(event -> back()));
        gui.setItem(1, 5, SEARCH_ITEM.asGuiItem(event -> {
            if (event.isRightClick()) {
                search("");
                return;
            }
            PromptManager.getInstance().prompt(player, "Search").thenAccept(this::search).exceptionally(throwable -> {
                search("");
                return null;
            });
        }));
        // Setup GUI items
        setupGui();
        // Next and Previous item
        GuiItems.setupNextPreviousItems(gui, 6, 3, 7);
    }

    public abstract void setupGui();

    protected abstract void searchInternal(String query);

    public void open() {
        gui.open(player);
    }

    public void search(String query) {
        search(query, false);
    }

    public void search(String query, boolean force) {
        if (this.query.equals(query) && !force) {
            // Same query, do nothing
            return;
        }
        searchInternal(query);
    }

    public void back() {
        // To be overridden
    }
}
