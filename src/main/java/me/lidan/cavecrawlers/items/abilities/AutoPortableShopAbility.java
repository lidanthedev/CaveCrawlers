package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonObject;
import dev.triumphteam.gui.components.util.ItemNbt;
import me.lidan.cavecrawlers.shop.ShopItem;
import me.lidan.cavecrawlers.shop.ShopManager;
import me.lidan.cavecrawlers.shop.ShopMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AutoPortableShopAbility extends PortableShopAbility {
    public static final String PORTABLE_SHOP_ITEM = "slotId";
    public static final int NO_SLOT_ID = -1;
    protected boolean silent = true;
    private int slotId = NO_SLOT_ID;

    public AutoPortableShopAbility(String name, String description, double cost, long cooldown) {
        super(name, description, cost, cooldown);
    }

    public AutoPortableShopAbility() {
        this("Auto Portable Shop", "Open a portable shop. allowing you to select items to buy automatically by right clicking Requires Rank", 0, 1000);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onKill(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();
        if (player == null) {
            return;
        }
        tryToBuyAutomatically(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        tryToBuyAutomatically(player);
    }

    public void tryToBuyAutomatically(Player player) {
        if (!player.hasPermission("cavecrawlers.portableshop.auto")) {
            return;
        }

        List<ItemStack> items = getItemsWithAbility(player);
        for (ItemStack item : items) {
            buyAutomatically(player, item);
        }
    }

    protected void buyAutomatically(Player player, ItemStack item) {
        String shopId = getShopIdOfItem(item);
        if (shopId == null) {
            return;
        }
        ShopMenu shopMenu = ShopManager.getInstance().getShop(shopId);
        if (shopMenu == null) {
            return;
        }
        int slotId = getSlotIdOfItem(item);
        if (slotId == NO_SLOT_ID) {
            return;
        }
        ShopItem shopItem = shopMenu.getShopItemList().get(slotId);
        if (shopItem.canBuy(player)) {
            shopItem.buy(player, silent);
        }
    }

    protected int getSlotIdOfItem(ItemStack item) {
        if (slotId != NO_SLOT_ID) {
            return slotId;
        }
        String itemSlotStr = ItemNbt.getString(item, PORTABLE_SHOP_ITEM);
        if (itemSlotStr == null) {
            return NO_SLOT_ID;
        }
        try {
            return Integer.parseInt(itemSlotStr);
        } catch (NumberFormatException e) {
            return NO_SLOT_ID;
        }
    }

    public List<ItemStack> getItemsWithAbility(Player player) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && hasAbility(item)) {
                items.add(item);
            }
        }
        return items;
    }

    @Override
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        AutoPortableShopAbility ability = (AutoFullShopAbility) super.buildAbilityWithSettings(map);
        if (map.has("slotId")) {
            ability.slotId = map.get("slotId").getAsInt();
        }
        if (map.has("silent")) {
            ability.silent = map.get("silent").getAsBoolean();
        }
        return ability;
    }
}
