package me.lidan.cavecrawlers.shop;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.util.ItemNbt;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import lombok.Data;
import lombok.NonNull;
import me.lidan.cavecrawlers.gui.GuiItems;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.abilities.AutoPortableShopAbility;
import me.lidan.cavecrawlers.items.abilities.PortableShopAbility;
import me.lidan.cavecrawlers.shop.editor.ShopItemEditor;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ShopMenu implements ConfigurationSerializable {
    private String title;
    private List<ShopItem> shopItemList;
    private String id;

    public ShopMenu(String title, List<ShopItem> shopItemList) {
        this.title = title;
        this.shopItemList = shopItemList;
    }

    public PaginatedGui buildGui() {
        PaginatedGui gui = Gui.paginated().title(MiniMessageUtils.miniMessage("<title>", Map.of("title", title))).rows(6).pageSize(28).disableAllInteractions().create();
        gui.getFiller().fillBorder(ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Component.text("")).asGuiItem());
        for (int i = 0; i < shopItemList.size(); i++) {
            ShopItem shopItem = shopItemList.get(i);
            int slotId = i;
            GuiItem guiItem = ItemBuilder.from(shopItem.toItem()).asGuiItem(event -> {
                if (event.getWhoClicked() instanceof Player player) {
                    if (event.getAction() == InventoryAction.CLONE_STACK && player.hasPermission("cavecrawlers.admin")) {
                        new ShopItemEditor(player, this, shopItem).open();
                        return;
                    }
                    if (event.isRightClick() && player.hasPermission("cavecrawlers.portableshop.auto")) {
                        portableShopCraft(player, shopItem, slotId);
                        return;
                    }
                    boolean buy = shopItem.buy(player);
                    if (!buy) {
                        player.sendMessage(ChatColor.RED + "You don't have the items or coins!");
                    }
                }
            });
            gui.addItem(guiItem);
            GuiItems.setupNextPreviousItems(gui, gui.getRows());
        }
        return gui;
    }

    public void open(Player player) {
        PaginatedGui gui = buildGui();
        gui.open(player);
        portableShop(player);
    }

    public void portableShop(Player player) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        ItemInfo itemInfo = ItemsManager.getInstance().getItemFromItemStackSafe(itemStack);
        if (itemInfo == null) {
            return;
        }
        if (itemInfo.getAbility() instanceof PortableShopAbility portableShopAbility) {
            if (portableShopAbility.getShopId() != null) {
                // Already set manually by admin
                return;
            }
            String oldShop = ItemNbt.getString(itemStack, PortableShopAbility.PORTABLE_SHOP_ID);
            if (oldShop != null && oldShop.equals(id)) {
                // Already set to this shop
                return;
            }
            ItemNbt.setString(itemStack, PortableShopAbility.PORTABLE_SHOP_ID, id);
            if (ItemNbt.getString(itemStack, AutoPortableShopAbility.PORTABLE_SHOP_ITEM) != null) {
                ItemNbt.removeTag(itemStack, AutoPortableShopAbility.PORTABLE_SHOP_ITEM);
            }
            player.sendMessage("Portable shop set to " + title);
        }
    }

    private void portableShopCraft(Player player, ShopItem shopItem, int slotId) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        ItemInfo itemInfo = ItemsManager.getInstance().getItemFromItemStackSafe(itemStack);
        if (itemInfo == null) {
            return;
        }
        if (itemInfo.getAbility() instanceof AutoPortableShopAbility portableShopAbility) {
            if (portableShopAbility.getShopId() != null) {
                // Already set manually by admin
                return;
            }
            ItemNbt.setString(itemStack, AutoPortableShopAbility.PORTABLE_SHOP_ITEM, String.valueOf(slotId));
            player.sendMessage("Portable shop item set to " + shopItem.getResult().getFormattedName());
            player.closeInventory();
        }
    }

    @NonNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        List<Map<String, Object>> itemList = new ArrayList<>();
        for (ShopItem shopItem : shopItemList) {
            itemList.add(shopItem.serialize());
        }
        map.put("items", itemList);
        return map;
    }

    public static ShopMenu deserialize(Map<String, Object> map) {
        String title = (String) map.get("title");

        List<ShopItem> items = new ArrayList<>();
        List<Map<String, Object>> itemList = (List<Map<String, Object>>) map.get("items");
        for (Map<String, Object> itemMap : itemList) {
            ShopItem item = ShopItem.deserialize(itemMap);
            items.add(item);
        }

        return new ShopMenu(title, items);
    }

}
