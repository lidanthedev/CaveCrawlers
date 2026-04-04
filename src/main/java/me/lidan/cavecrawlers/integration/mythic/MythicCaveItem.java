package me.lidan.cavecrawlers.integration.mythic;

import io.lumine.mythic.api.adapters.AbstractItemStack;
import io.lumine.mythic.api.drops.DropMetadata;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.items.MythicItem;
import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.items.ItemsManager;
import org.bukkit.inventory.ItemStack;

@Slf4j
public class MythicCaveItem extends MythicItem {
    private static final ItemsManager itemsManager = ItemsManager.getInstance();

    public MythicCaveItem(String internalName) {
        super(CaveCrawlers.getInstance().getMythicBukkit().getPackManager().getBasePack(), null, internalName, null);
    }

    @Override
    public void loadItem() {

    }

    @Override
    public AbstractItemStack generateItemStack(DropMetadata meta, int amount) {
        ItemStack itemStack = itemsManager.buildItem(this.getInternalName(), amount);
        return BukkitAdapter.adapt(itemStack);
    }
}
