package me.lidan.cavecrawlers.integration.mythic;

import io.lumine.mythic.api.adapters.AbstractItemStack;
import io.lumine.mythic.api.drops.DropMetadata;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.items.MythicItem;
import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.items.ItemsManager;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
public class MythicCaveItem extends MythicItem {
    private static final ItemsManager itemsManager = ItemsManager.getInstance();
    private static final Method adaptMethod;

    static {
        try {
            adaptMethod = BukkitAdapter.class.getDeclaredMethod("adapt", ItemStack.class);
            ;
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new RuntimeException("Failed to get adaptMethod", e);
        }
    }

    public MythicCaveItem(String internalName) {
        super(CaveCrawlers.getInstance().getMythicBukkit().getPackManager().getBasePack(), null, internalName, null);
    }

    @Override
    public void loadItem() {

    }

    @Override
    public AbstractItemStack generateItemStack(DropMetadata meta, int amount) {
        ItemStack itemStack = itemsManager.buildItem(this.getInternalName(), amount);
        AbstractItemStack adapt = null;
        try {
            adapt = (AbstractItemStack) adaptMethod.invoke(null, itemStack.clone());
        } catch (IllegalAccessException | InvocationTargetException | ClassCastException e) {
            log.error("generateItemStack: Failed to generate item stack", e);
            throw new RuntimeException("Failed to generate item stack", e);
        }
        return adapt;
    }
}
