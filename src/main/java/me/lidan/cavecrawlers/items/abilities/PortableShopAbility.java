package me.lidan.cavecrawlers.items.abilities;

import dev.triumphteam.gui.components.util.ItemNbt;
import me.lidan.cavecrawlers.shop.ShopManager;
import me.lidan.cavecrawlers.shop.ShopMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

public class PortableShopAbility extends ClickAbility {
    public static final String PORTABLE_SHOP_ID = "shopId";

    public PortableShopAbility() {
        super("Port a Shop", "Open a portable shop", 0, 1000);
    }

    public PortableShopAbility(String name, String description, double cost, long cooldown) {
        super(name, description, cost, cooldown);
    }

    @Override
    protected boolean useAbility(PlayerEvent playerEvent) {
        Player player = playerEvent.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        String shopId = ItemNbt.getString(item, PORTABLE_SHOP_ID);
        if (!player.hasPermission("cavecrawlers.portableshop")) {
            player.sendMessage("You don't have permission to use this item");
            return false;
        }
        if (shopId == null) {
            player.sendMessage("No shop set");
            return false;
        }
        ShopMenu shopMenu = ShopManager.getInstance().getShop(shopId);
        if (shopMenu == null) {
            player.sendMessage("Shop not found");
            return false;
        }
        shopMenu.getGui().open(player);
        return true;
    }
}
