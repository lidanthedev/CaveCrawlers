package me.lidan.cavecrawlers.shop;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.util.ItemNbt;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import lombok.Data;
import lombok.Getter;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.abilities.PortableShopAbility;
import me.lidan.cavecrawlers.utils.JsonMessage;
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
                    if (event.isRightClick() && player.hasPermission("cavecrawlers.portableshop.craft")){
                        portableShopCraft(player, shopItem, slotId);
                        return;
                    }
                    boolean buy = shopItem.buy(player);
                    if (!buy) {
                        player.sendMessage(ChatColor.RED + "You don't have the items!");
                    }
                }
            });
            gui.addItem(guiItem);
        }
    }

    private void portableShopCraft(Player player, ShopItem shopItem, int slotId) {

    }

    public void shopEditor(Player player, ShopItem shopItem, int slotId) {
        JsonMessage message = new JsonMessage();
        String s = "Editing item: %s\n".formatted(shopItem.formatName(shopItem.getResult().getFormattedName(), shopItem.getResultAmount()));
        message.append(ChatColor.GOLD.toString() + ChatColor.BOLD + s).save();
        Map<ItemInfo, Integer> itemsMap = shopItem.getItemsMap();
        for (ItemInfo itemInfo : itemsMap.keySet()) {
            int amount = itemsMap.get(itemInfo);
            String suggestion = "/ct shop update " + id + " " + slotId + " " + itemInfo.getID() + " " + amount;
            String msg = shopItem.formatName(itemInfo.getFormattedName(), amount) + ChatColor.GOLD + ChatColor.BOLD + " CLICK TO EDIT\n";
            message.append(msg).setHoverAsTooltip("Edit").setClickAsSuggestCmd(suggestion).save();
        }
        String suggestion = "/ct shop updatecoins " + id + " " + slotId + " ";
        message.append(ChatColor.GOLD.toString() + ChatColor.BOLD + "Set Coins").setHoverAsTooltip("Set Coins").setClickAsSuggestCmd(suggestion).save();
        suggestion = "/ct shop update " + id + " " + slotId + " ";
        message.append(ChatColor.GREEN.toString() + ChatColor.BOLD + " New ingredient").setHoverAsTooltip("Add").setClickAsSuggestCmd(suggestion).save();
        suggestion = "/ct shop remove " + id + " " + slotId;
        message.append(ChatColor.RED.toString() + ChatColor.BOLD + " Remove item").setHoverAsTooltip("Remove").setClickAsSuggestCmd(suggestion).save();
        message.send(player);
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
            ItemNbt.setString(itemStack, PortableShopAbility.PORTABLE_SHOP, id);
            player.sendMessage("Portable shop set to " + title);
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
