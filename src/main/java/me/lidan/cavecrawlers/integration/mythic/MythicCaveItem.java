package me.lidan.cavecrawlers.integration.mythic;

import io.lumine.mythic.api.adapters.AbstractItemStack;
import io.lumine.mythic.api.drops.DropMetadata;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.items.MythicItem;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import org.bukkit.inventory.ItemStack;

@Slf4j
public class MythicCaveItem extends MythicItem {
    private static final ItemsManager itemsManager = ItemsManager.getInstance();
    private final ItemInfo itemInfo;

    public MythicCaveItem(String internalName, @NonNull ItemInfo itemInfo) {
        super(CaveCrawlers.getInstance().getMythicBukkit().getPackManager().getBasePack(), null, internalName, null);
        this.itemInfo = itemInfo;
    }

    @Override
    public void loadItem() {
        // we override this method to prevent loading the item from the config
    }

    @Override
    public AbstractItemStack generateItemStack(DropMetadata meta, int amount) {
        ItemStack itemStack = itemsManager.buildItem(itemInfo, amount);
        return BukkitAdapter.adapt(itemStack);
    }
}
