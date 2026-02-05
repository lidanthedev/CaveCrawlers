package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonObject;
import dev.triumphteam.gui.components.util.ItemNbt;
import lombok.Getter;
import me.lidan.cavecrawlers.shop.ShopManager;
import me.lidan.cavecrawlers.shop.ShopMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortableShopAbility extends ClickAbility {
    public static final String PORTABLE_SHOP_ID = "shopId";
    private static final Logger log = LoggerFactory.getLogger(PortableShopAbility.class);
    @Getter
    private String shopId;

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
        String shopId = getShopIdOfItem(item);
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
        shopMenu.open(player);
        return true;
    }

    protected String getShopIdOfItem(ItemStack item) {
        String shopId = getShopId();
        return shopId != null ? shopId : ItemNbt.getString(item, PORTABLE_SHOP_ID);
    }

    @Override
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        PortableShopAbility ability = (PortableShopAbility) super.buildAbilityWithSettings(map);
        if (map.has("shopId")) {
            ability.shopId = map.get("shopId").getAsString();
        }
        return ability;
    }
}
