package me.lidan.cavecrawlers.shop.editor;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.lidan.cavecrawlers.gui.GuiItems;
import me.lidan.cavecrawlers.gui.ItemsGui;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.prompt.PromptManager;
import me.lidan.cavecrawlers.shop.ShopItem;
import me.lidan.cavecrawlers.shop.ShopManager;
import me.lidan.cavecrawlers.shop.ShopMenu;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
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
                .rows(4)
                .create();
        this.shopMenu = shopMenu;
        this.shopItem = shopItem;
        gui.disableAllInteractions();
        gui.getFiller().fillBorder(GuiItems.GLASS_ITEM);
        gui.setItem(4, 5, ItemBuilder.from(Material.LIME_CONCRETE).name(MiniMessageUtils.miniMessage("<green>New Ingredient")).asGuiItem(inventoryClickEvent -> {
            new ItemsGui(player, "", (event, itemInfo) -> {
                shopManager.updateShop(shopMenu, shopItem, itemInfo, 1);
                reopen();
            }, MiniMessageUtils.miniMessage("<blue>Items Browser")).open();
        }));
        gui.setItem(4, 1, GuiItems.BACK_ITEM.asGuiItem(event -> {
            new ShopItemEditor(player, shopMenu, shopItem).open();
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
            List<Component> lore = new ArrayList<>(item.lore());
            lore.add(Component.empty());
            lore.add(MiniMessageUtils.miniMessage("<gold>Click to edit amount"));
            lore.add(MiniMessageUtils.miniMessage("<gold>Right click to remove ingredient"));
            ItemBuilder itemBuilder = ItemBuilder.from(item)
                    .setName(ShopItem.formatName(itemInfo.getFormattedName(), amount))
                    .lore(lore);
            GuiItem guiItem = itemBuilder.asGuiItem(event -> {
                if (event.getWhoClicked() instanceof Player player) {
                    if (event.isRightClick()) {
                        shopManager.updateShop(shopMenu, shopItem, itemInfo, 0);
                        reopen();
                    } else if (event.isLeftClick()) {
                        PromptManager.getInstance().prompt(player, "Enter new amount").thenAccept(input -> {
                            try {
                                int newAmount = Integer.parseInt(input);
                                if (newAmount < 0) {
                                    player.sendMessage(MiniMessageUtils.miniMessage("<red>Amount cannot be negative!"));
                                    return;
                                }
                                shopManager.updateShop(shopMenu, shopItem, itemInfo, newAmount);
                                reopen();
                            } catch (NumberFormatException e) {
                                player.sendMessage(MiniMessageUtils.miniMessage("<red>Invalid amount! Please enter a valid number."));
                            }
                        });
                    }
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
