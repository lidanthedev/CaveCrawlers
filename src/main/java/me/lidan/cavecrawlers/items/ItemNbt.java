package me.lidan.cavecrawlers.items;

import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Item persistent-data helper.
 */
public final class ItemNbt {
    private ItemNbt() {
    }

    private static NamespacedKey key(String key) {
        return new NamespacedKey(CaveCrawlers.getInstance(), key.toLowerCase(java.util.Locale.ROOT));
    }

    public static String getString(ItemStack item, String key) {
        return item.getItemMeta().getPersistentDataContainer().get(key(key), PersistentDataType.STRING);
    }

    public static ItemStack setString(ItemStack item, String key, String value) {
        item.editMeta(meta -> meta.getPersistentDataContainer().set(key(key), PersistentDataType.STRING, value));
        return item;
    }

    public static void removeTag(ItemStack item, String key) {
        item.editMeta(meta -> meta.getPersistentDataContainer().remove(key(key)));
    }
}
