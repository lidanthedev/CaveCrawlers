package me.lidan.cavecrawlers.items.abilities;

import dev.triumphteam.gui.components.util.ItemNbt;
import me.lidan.cavecrawlers.shop.ShopManager;
import me.lidan.cavecrawlers.shop.ShopMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

public class PortableShopAbility extends ClickAbility {
    public static final String PORTABLE_SHOP = "shopId";

    public PortableShopAbility() {
        super("Port a Shop", "Open a portable shop", 0, 1000);
    }

    @Override
    protected void useAbility(PlayerEvent playerEvent) {
        Player player = playerEvent.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        String shopId = ItemNbt.getString(item, PORTABLE_SHOP);
        if (!player.hasPermission("cavecrawlers.portableshop")) {
            player.sendMessage("You don't have permission to use this item");
            return;
        }
        if (shopId == null) {
            player.sendMessage("No shop set");
            return;
        }
        ShopMenu shopMenu = ShopManager.getInstance().getShop(shopId);
        if (shopMenu == null) {
            player.sendMessage("Shop not found");
            return;
        }
        shopMenu.getGui().open(player);
    }
}
