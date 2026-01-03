package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.shop.ShopItem;
import me.lidan.cavecrawlers.shop.ShopManager;
import me.lidan.cavecrawlers.shop.ShopMenu;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AutoFullShopAbility extends AutoPortableShopAbility {
    public AutoFullShopAbility() {
        super("Auto Full Shop", "Open a portable shop. automatically buys all items. Requires Rank", 0, 1000);
    }

    @Override
    protected void buyAutomatically(Player player, ItemStack item) {
        String shopId = getShopIdOfItem(item);
        if (shopId == null) {
            return;
        }
        ShopMenu shopMenu = ShopManager.getInstance().getShop(shopId);
        if (shopMenu == null) {
            return;
        }
        for (ShopItem shopItem : shopMenu.getShopItemList()) {
            if (shopItem.canBuy(player)) {
                shopItem.buy(player, silent);
            }
        }
    }

}
