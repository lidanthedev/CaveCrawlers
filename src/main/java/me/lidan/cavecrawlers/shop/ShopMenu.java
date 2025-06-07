package me.lidan.cavecrawlers.shop;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.util.ItemNbt;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import lombok.Data;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.abilities.AutoPortableShopAbility;
import me.lidan.cavecrawlers.items.abilities.PortableShopAbility;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ShopMenu implements ConfigurationSerializable {
    private String title;
    private List<ShopItem> shopItemList;
    private Gui gui;
    private String id;

    public ShopMenu(String title, List<ShopItem> shopItemList) {
        this.title = title;
        this.shopItemList = shopItemList;
        buildGui();
    }

    public void buildGui(){
        this.gui = Gui.gui().title(Component.text(this.title)).rows(6).disableAllInteractions().create();
        gui.getFiller().fillBorder(ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Component.text("")).asGuiItem());
        for (int i = 0; i < shopItemList.size(); i++) {
            ShopItem shopItem = shopItemList.get(i);
            int slotId = i;
            GuiItem guiItem = ItemBuilder.from(shopItem.toItem()).asGuiItem(event -> {
                if (event.getWhoClicked() instanceof Player player) {
                    if (event.getAction() == InventoryAction.CLONE_STACK && player.hasPermission("cavecrawlers.admin")){
                        shopEditor(player, shopItem, slotId);
                        return;
                    }
                    if (event.isRightClick() && player.hasPermission("cavecrawlers.portableshop.auto")){
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
        }
    }

    public void shopEditor(Player player, ShopItem shopItem, int slotId) {
        String baseMessage = "<gold><bold>Editing item: <item_name>\n</bold></gold>";
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("item_name", shopItem.formatName(shopItem.getResult().getFormattedName(), shopItem.getResultAmount()));
        Component message = MiniMessageUtils.miniMessage(baseMessage, placeholders);

        Map<ItemInfo, Integer> itemsMap = shopItem.getItemsMap();
        for (ItemInfo itemInfo : itemsMap.keySet()) {
            int amount = itemsMap.get(itemInfo);
            String suggestion = "/ct shop update " + id + " " + slotId + " " + itemInfo.getID() + " " + amount;
            String itemMessage = "<item_name><hover:show_text:'Click to edit'><click:suggest_command:'<command>'><gold><bold> CLICK TO EDIT\n</bold></gold></click></hover>";
            Map<String, Object> itemPlaceholders = Map.of(
                    "item_name", shopItem.formatName(itemInfo.getFormattedName(), amount),
                    "command", suggestion
            );
            message = message.append(MiniMessageUtils.miniMessage(itemMessage, itemPlaceholders));
        }

        String coinsSuggestion = "/ct shop updatecoins " + id + " " + slotId + " ";
        message = message.append(MiniMessageUtils.miniMessage(
                "<hover:show_text:'Set coins'><click:suggest_command:'<command>'><gold><bold>Set Coins</bold></gold></click></hover>",
                Map.of("command", coinsSuggestion)
        ));

        String newIngredientSuggestion = "/ct shop update " + id + " " + slotId + " ";
        message = message.append(MiniMessageUtils.miniMessage(
                "<hover:show_text:'Add new ingredient'><click:suggest_command:'<command>'><green><bold> New ingredient</bold></green></click></hover>",
                Map.of("command", newIngredientSuggestion)
        ));

        String removeItemSuggestion = "/ct shop remove " + id + " " + slotId;
        message = message.append(MiniMessageUtils.miniMessage(
                "<hover:show_text:'Remove item'><click:suggest_command:'<command>'><red><bold> Remove item</bold></red></click></hover>",
                Map.of("command", removeItemSuggestion)
        ));

        player.sendMessage(message);
        player.closeInventory();
    }

    public void open(Player player){
        gui.open(player);
        portableShop(player);
    }

    public void portableShop(Player player){
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        ItemInfo itemInfo = ItemsManager.getInstance().getItemFromItemStackSafe(itemStack);
        if (itemInfo == null){
            return;
        }
        if (itemInfo.getAbility() instanceof PortableShopAbility portableShopAbility){
            ItemNbt.setString(itemStack, PortableShopAbility.PORTABLE_SHOP_ID, id);
            if (ItemNbt.getString(itemStack, AutoPortableShopAbility.PORTABLE_SHOP_ITEM) != null){
                ItemNbt.removeTag(itemStack, AutoPortableShopAbility.PORTABLE_SHOP_ITEM);
            }
            player.sendMessage("Portable shop set to " + title);
        }
    }

    private void portableShopCraft(Player player, ShopItem shopItem, int slotId) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        ItemInfo itemInfo = ItemsManager.getInstance().getItemFromItemStackSafe(itemStack);
        if (itemInfo == null){
            return;
        }
        if (itemInfo.getAbility() instanceof AutoPortableShopAbility portableShopAbility){
            ItemNbt.setString(itemStack, AutoPortableShopAbility.PORTABLE_SHOP_ITEM, String.valueOf(slotId));
            player.sendMessage("Portable shop item set to " + shopItem.getResult().getFormattedName());
            player.closeInventory();
        }
    }

    @NotNull
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
