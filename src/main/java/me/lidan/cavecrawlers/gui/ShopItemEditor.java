package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import me.lidan.cavecrawlers.prompt.PromptManager;
import me.lidan.cavecrawlers.shop.ShopItem;
import me.lidan.cavecrawlers.shop.ShopManager;
import me.lidan.cavecrawlers.shop.ShopMenu;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class ShopItemEditor {
    public static final int SLOT_NOT_FOUND = -1;
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

        // Initialize GUI items and actions here
        initializeGuiItems();
    }

    private void initializeGuiItems() {
        gui.setItem(2, 5, ItemBuilder.from(shopItem.toItem()).asGuiItem());
        gui.setItem(4, 1, GuiItems.BACK_ITEM.asGuiItem(event -> {
            new ShopEditor(player, shopMenu).open();
        }));
        gui.setItem(4, 3, ItemBuilder.from(Material.YELLOW_CONCRETE).name(MiniMessageUtils.miniMessage("<gold>Set Coins")).asGuiItem(event -> {
            PromptManager.getInstance().prompt(player, "Enter new coin price").thenAccept(input -> {
                try {
                    int coins = Integer.parseInt(input);
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
                    reopen();
                } catch (NumberFormatException e) {
                    player.sendMessage(MiniMessageUtils.miniMessage("<red>Invalid number!"));
                }
            });
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
