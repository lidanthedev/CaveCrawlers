package me.lidan.cavecrawlers.integration.mythic;

import io.lumine.mythic.api.items.ItemSupplier;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.items.ItemsManager;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Objects;

public class MythicItemSupport implements ItemSupplier {
    @Override
    public String getNamespace() {
        return "cavecrawlers";
    }

    @Override
    public ItemStack getItem(String s) {
        return CaveCrawlers.getAPI().getItemsAPI().buildItem(s, 1);
    }

    @Override
    public boolean isSimilar(String s, ItemStack itemStack) {
        return Objects.equals(ItemsManager.getInstance().getIDofItemStackSafe(itemStack), s);
    }

    @Override
    public Collection<String> getAvailableItemNames() {
        return ItemsManager.getInstance().getKeys();
    }
}
