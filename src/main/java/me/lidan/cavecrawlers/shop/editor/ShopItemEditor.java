package me.lidan.cavecrawlers.shop.editor;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import me.lidan.cavecrawlers.gui.ConfirmGui;
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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ShopItemEditor {
    public static final int SLOT_NOT_FOUND = -1;
    public static final int MIN_ITEM_AMOUNT = 1;
    private final Player player;
    private final Gui gui;
    private final ShopMenu shopMenu;
    private final ShopItem shopItem;
    private final ShopManager shopManager = ShopManager.getInstance();

    public ShopItemEditor(Player player, ShopMenu shopMenu, ShopItem shopItem) {
        this.player = player;
        this.gui = Gui.gui()
                .title(MiniMessageUtils.miniMessage("Shop Item Editor"))
                .rows(4)
                .create();
        this.shopMenu = shopMenu;
        this.shopItem = shopItem;
        gui.disableAllInteractions();
        gui.getFiller().fill(GuiItems.GLASS_ITEM);

        initGui();
    }

    private void initGui() {
        ItemStack resultItem = shopItem.toItem();
        List<Component> lore = ShopEditor.changeLastLoreLine(resultItem, MiniMessageUtils.miniMessage("<yellow>Click to edit item"));
        lore.add(MiniMessageUtils.miniMessage("<yellow>Right click to change amount"));
        gui.setItem(2, 5, ItemBuilder.from(resultItem).lore(lore).asGuiItem(event -> {
            if (event.getClick() == ClickType.LEFT) {
                new ItemsGui(player, "", (event1, itemInfo) -> {
                    shopItem.setResult(itemInfo);
                    shopManager.saveShop(shopMenu.getId(), shopMenu);
                    reopen();
                }, MiniMessageUtils.miniMessage("<blue>Change Item")).open();
            }
            if (event.getClick() == ClickType.RIGHT) {
                PromptManager.getInstance().promptNumber(player, "Enter new amount").thenAccept(amount -> {
                    if (amount < MIN_ITEM_AMOUNT) {
                        player.sendMessage(MiniMessageUtils.miniMessage("<red>Amount must be at least 1!"));
                        return;
                    }
                    shopItem.setResultAmount(amount);
                    shopManager.saveShop(shopMenu.getId(), shopMenu);
                }).exceptionally(throwable -> {
                    player.sendMessage(MiniMessageUtils.miniMessage("<red>Invalid number!"));
                    return null;
                }).whenComplete((unused, throwable) -> reopen());
            }
        }));
        gui.setItem(4, 1, GuiItems.BACK_ITEM.asGuiItem(event -> {
            new ShopEditor(player, shopMenu).open();
        }));
        gui.setItem(4, 3, ItemBuilder.from(Material.YELLOW_CONCRETE).name(MiniMessageUtils.miniMessage("<gold>Set Coins")).asGuiItem(event -> {
            PromptManager.getInstance().promptNumber(player, "Enter new coin price").thenAccept(coins -> {
                if (coins < 0) {
                    player.sendMessage(MiniMessageUtils.miniMessage("<red>Coins cannot be negative!"));
                    return;
                }
                int slotID = shopMenu.getShopItemList().indexOf(shopItem);
                if (slotID == SLOT_NOT_FOUND) {
                    player.sendMessage(MiniMessageUtils.miniMessage("<red>Shop item not found!"));
                    return;
                }
                shopManager.updateShopCoins(shopMenu.getId(), slotID, coins);
            }).exceptionally(throwable -> {
                player.sendMessage(MiniMessageUtils.miniMessage("<red>Invalid number!"));
                return null;
            }).whenComplete((unused, throwable) -> reopen());
        }));
        gui.setItem(4, 5, ItemBuilder.from(Material.OAK_SIGN).name(MiniMessageUtils.miniMessage("<gold>Edit Ingredients")).asGuiItem(event -> {
            new ShopItemIngredientsEditor(player, shopMenu, shopItem).open();
        }));
        gui.setItem(4, 7, ItemBuilder.from(Material.RED_CONCRETE).name(MiniMessageUtils.miniMessage("<red>Delete Item")).asGuiItem(event -> {
            new ConfirmGui(player, MiniMessageUtils.miniMessage("<red>Delete Item?"), () -> {
                int slotID = shopMenu.getShopItemList().indexOf(shopItem);
                if (slotID == SLOT_NOT_FOUND) {
                    player.sendMessage(MiniMessageUtils.miniMessage("<red>Shop item not found!"));
                    return;
                }
                shopManager.removeShopItem(shopMenu, slotID);
                player.sendMessage(MiniMessageUtils.miniMessage("<green>Shop item deleted successfully!"));
                gui.close(player);
            }, () -> {
                player.sendMessage(MiniMessageUtils.miniMessage("<red>Deletion cancelled."));
                reopen();
            }).open();
        }));
    }

    private void reopen() {
        new ShopItemEditor(player, shopMenu, shopItem).open();
    }

    public void open() {
        gui.open(player);
    }
}
