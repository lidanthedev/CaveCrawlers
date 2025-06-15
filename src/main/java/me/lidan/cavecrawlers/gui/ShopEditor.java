package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
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

public class ShopEditor {
    public static final int DEFAULT_COIN_PRICE = 10;
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
    }

    public static @NotNull List<Component> changeLastLoreLine(ItemStack item, Component component) {
        List<Component> lore = item.lore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        lore.remove(lore.size() - 1);
        lore.add(component);
        return lore;
    }

    public void open() {
        gui.open(player);
    }
}
