package me.lidan.cavecrawlers.shop.editor;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.lidan.cavecrawlers.gui.GuiItems;
import me.lidan.cavecrawlers.gui.ItemsGui;
import me.lidan.cavecrawlers.prompt.PromptManager;
import me.lidan.cavecrawlers.shop.ShopItem;
import me.lidan.cavecrawlers.shop.ShopManager;
import me.lidan.cavecrawlers.shop.ShopMenu;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopEditor {
    public static final int DEFAULT_COIN_PRICE = 0;
    private final Player player;
    private final Gui gui;
    private final ShopMenu shopMenu;
    private final ShopManager shopManager = ShopManager.getInstance();

    public ShopEditor(Player player, ShopMenu shopMenu) {
        this.player = player;
        this.gui = Gui.gui()
                .title(MiniMessageUtils.miniMessage("Shop Editor: " + shopMenu.getId()))
                .rows(6)
                .create();
        this.shopMenu = shopMenu;
        gui.disableAllInteractions();
        gui.getFiller().fillBorder(GuiItems.GLASS_ITEM);

        // Initialize GUI items and actions here
        initializeGuiItems();
    }

    private void initializeGuiItems() {
        List<ShopItem> shopItemList = shopMenu.getShopItemList();
        for (ShopItem shopItem : shopItemList) {
            ItemStack item = shopItem.toItem();
            List<Component> lore = changeLastLoreLine(item, MiniMessageUtils.miniMessage("<yellow>Click to edit item"));
            GuiItem guiItem = ItemBuilder.from(item).lore(lore).asGuiItem(event -> {
                new ShopItemEditor(player, shopMenu, shopItem).open();
            });
            gui.addItem(guiItem);
        }
        gui.setItem(6, 5, ItemBuilder.from(Material.GREEN_CONCRETE).name(MiniMessageUtils.miniMessage("<green>Add New Item")).asGuiItem(event -> {
            new ItemsGui(player, "", (event1, itemInfo) -> {
                ShopItem shopItem = new ShopItem(itemInfo, 1, DEFAULT_COIN_PRICE, new HashMap<>());
                shopManager.addItemToShop(shopMenu.getId(), shopItem);
                new ShopItemEditor(player, shopMenu, shopItem).open();
            }, MiniMessageUtils.miniMessage("<green>Add item")).open();
        }));
        gui.setItem(1, 5, ItemBuilder.from(Material.OAK_SIGN).name(MiniMessageUtils.miniMessage("<gold>Shop name: <name>", Map.of("name", shopMenu.getTitle()))).lore(Component.empty(), MiniMessageUtils.miniMessage("<yellow>Click to change")).asGuiItem(event -> {
            PromptManager.getInstance().prompt(player, "Enter new shop name").thenAccept(input -> {
                shopMenu.setTitle(input);
                shopManager.saveShop(shopMenu.getId(), shopMenu);
                reopen();
            });
        }));
    }

    private void reopen() {
        new ShopEditor(player, shopMenu).open();
    }

    public static @NotNull List<Component> changeLastLoreLine(ItemStack item, Component component) {
        List<Component> lore = item.lore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        if (!lore.isEmpty()) {
            lore.remove(lore.size() - 1);
        }
        lore.add(component);
        return lore;
    }

    public void open() {
        gui.open(player);
    }
}
