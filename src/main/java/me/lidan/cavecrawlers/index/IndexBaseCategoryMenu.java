package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import me.lidan.cavecrawlers.gui.GuiItems;
import me.lidan.cavecrawlers.prompt.PromptManager;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public abstract class IndexBaseCategoryMenu {
    public static final String CAVECRAWLERS_INDEX_ADMIN_PERMISSION = "cavecrawlers.index.admin";
    public static ItemBuilder INDEX_GUIDE = ItemBuilder.from(Material.BOOK).name(MiniMessageUtils.miniMessage("<yellow>Index Guide")).lore(MiniMessageUtils.miniMessageList("<yellow>How to Read the drop</yellow>", "<gold>- [Amount] [Drop] ([chance]) [chance modifer] [amount modifer]</gold>", "<yellow>Example:", "<gray>- <gray>1-2 <white>Gold Ingot<gray> (<green>10.00%<gray>) <aqua>✯ <red>✘", "<yellow>You can get 1-2 drops of Gold Ingot", "<yellow>it has 10% and boosted by <aqua>✯ Magic Find", "<yellow>but no amount modifier</yellow>", "<red>✘ means no stat</red>"));
    public static ItemBuilder SEARCH_ITEM = ItemBuilder.from(Material.COMPASS).name(MiniMessageUtils.miniMessage("<blue>Search Index")).lore(MiniMessageUtils.miniMessageList("<yellow>Click to Search the Index", "<yellow>Right click to clear search"));
    protected final Player player;
    protected final IndexCategory category;
    protected final PaginatedGui gui;
    protected final String query;
    protected final IndexItemGenerator itemGenerator;

    public IndexBaseCategoryMenu(Player player, IndexCategory category, String query) {
        this.player = player;
        this.category = category;
        this.itemGenerator = IndexItemGenerator.getInstance();
        this.gui = Gui.paginated()
                .title(category.getTitle())
                .rows(6)
                .pageSize(28)
                .create();
        this.query = query.toLowerCase();
        gui.disableAllInteractions();
        // filler
        gui.getFiller().fillBorder(GuiItems.GLASS_ITEM);
        // Close item
        gui.setItem(6, 5, GuiItems.BACK_ITEM.asGuiItem(event -> new IndexMainMenu(player).open()));
        gui.setItem(6, 9, INDEX_GUIDE.asGuiItem());
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

    public void addItem(String entry, GuiItem item) {
        String fullEntry = category.name() + ":" + entry;
        if (itemGenerator.isHiddenEntry(fullEntry)) {
            return;
        }
        if (!player.hasPermission(CAVECRAWLERS_INDEX_ADMIN_PERMISSION)) {
            gui.addItem(item);
            return;
        }
        ItemBuilder itemBuilder = ItemBuilder.from(item.getItemStack());
        List<Component> lore = new ArrayList<>();
        List<Component> originalLore = item.getItemStack().lore();
        if (originalLore != null) {
            lore.addAll(originalLore);
        }
        lore.add(Component.empty());
        lore.add(MiniMessageUtils.miniMessage("<yellow>Right click to hide entry from index"));
        itemBuilder.lore(lore);
        GuiItem guiItem = itemBuilder.asGuiItem(event -> {
            if (!player.hasPermission(CAVECRAWLERS_INDEX_ADMIN_PERMISSION)) {
                return;
            }
            if (event.isRightClick()) {
                itemGenerator.toggleHiddenEntry(fullEntry);
                player.sendMessage(MiniMessageUtils.miniMessage("<yellow>Entry <gray>" + entry + " <yellow>is now <red>hidden<yellow> from the index."));
                search(query, true);
                return;
            }
            player.sendMessage(MiniMessageUtils.miniMessage("<red>Editing is not supported yet."));
        });
        gui.addItem(guiItem);
    }

    public abstract void setupGui();

    public void open() {
        gui.open(player);
    }


    public void search(String query) {
        search(query, false);
    }

    public abstract void search(String query, boolean force);
}
