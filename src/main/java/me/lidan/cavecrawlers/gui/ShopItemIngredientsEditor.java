package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.shop.ShopItem;
import me.lidan.cavecrawlers.shop.ShopManager;
import me.lidan.cavecrawlers.shop.ShopMenu;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ShopItemIngredientsEditor {
    private final Player player;
    private final Gui gui;
    private final ShopMenu shopMenu;
    private final ShopItem shopItem;
    private final ShopManager shopManager = ShopManager.getInstance();

    public ShopItemIngredientsEditor(Player player, ShopMenu shopMenu, ShopItem shopItem) {
        this.player = player;
        this.gui = Gui.gui()
                .title(MiniMessageUtils.miniMessage("Shop Item Ingredients"))
                .rows(6)
                .create();
        this.shopMenu = shopMenu;
        this.shopItem = shopItem;
        gui.disableAllInteractions();
        gui.getFiller().fillBottom(ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Component.text("")).asGuiItem());
        gui.setItem(6, 5, ItemBuilder.from(Material.LIME_CONCRETE).name(MiniMessageUtils.miniMessage("<green>New Ingredient")).asGuiItem(inventoryClickEvent -> {
            new ItemsGui(player, "", (event, itemInfo) -> {
                shopManager.updateShop(shopMenu, shopItem, itemInfo, 1);
                reopen();
            }, MiniMessageUtils.miniMessage("<blue>Items Browser")).open();
        }));
    }

    public void reopen() {
        new ShopItemIngredientsEditor(player, shopMenu, shopItem).open();
    }

    public void update() {
        for (Map.Entry<ItemInfo, Integer> entry : shopItem.getItemsMap().entrySet()) {
            ItemInfo itemInfo = entry.getKey();
            int amount = entry.getValue();
            ItemStack item = ItemsManager.getInstance().buildItem(itemInfo, amount);
            ItemBuilder itemBuilder = ItemBuilder.from(item)
                    .setName(ShopItem.formatName(itemInfo.getFormattedName(), amount))
                    .lore(MiniMessageUtils.miniMessage("<gray>Click to remove this ingredient"));
            GuiItem guiItem = itemBuilder.asGuiItem(event -> {
                if (event.getWhoClicked() instanceof Player player) {
                    shopManager.updateShop(shopMenu, shopItem, itemInfo, 0);
                    reopen();
                }
            });
            gui.addItem(guiItem);
        }
        gui.update();
    }

    public void open() {
        update();
        gui.open(player);
    }
}
