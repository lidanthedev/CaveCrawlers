package me.lidan.cavecrawlers.integration.mythic;

import io.lumine.mythic.api.adapters.AbstractItemStack;
import io.lumine.mythic.api.drops.DropMetadata;
import io.lumine.mythic.api.drops.IItemDrop;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.utils.numbers.Numbers;
import lombok.Getter;
import lombok.NonNull;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import org.bukkit.inventory.ItemStack;

public class MythicCaveDrop implements IItemDrop {
    private static final ItemsManager itemsManager = ItemsManager.getInstance();
    @Getter
    private final ItemInfo itemInfo;

    public MythicCaveDrop(@NonNull ItemInfo itemInfo) {
        this.itemInfo = itemInfo;
    }

    @Override
    public AbstractItemStack getDrop(DropMetadata dropMetadata, double amount) {
        ItemStack itemStack = itemsManager.buildItem(this.getItemInfo(), Numbers.round(amount));
        return BukkitAdapter.adapt(itemStack);
    }
}
