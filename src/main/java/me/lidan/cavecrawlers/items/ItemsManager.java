package me.lidan.cavecrawlers.items;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.util.ItemNbt;
import me.lidan.cavecrawlers.items.abilities.AbilityManager;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ItemsManager {
    private static ItemsManager instance;
    private final Map<String, ItemInfo> itemsMap;

    public ItemsManager() {
        itemsMap = new HashMap<>();
    }

    public void registerItem(String ID, ItemInfo info){
        info.setID(ID);
        itemsMap.put(ID, info);
    }

    public void registerExampleItems(){
        AbilityManager abilityManager = AbilityManager.getInstance();
        Stats stats = new Stats(true);
        stats.set(StatType.DAMAGE, 5);
        stats.set(StatType.STRENGTH, 15);
        registerItem("EXAMPLE_SWORD", new ItemInfo("Example Sword", stats, ItemType.SWORD, Material.WOODEN_SWORD, Rarity.COMMON));

        stats = new Stats(true);
        stats.set(StatType.DAMAGE, 5);
        stats.set(StatType.MINING_SPEED, 50);
        registerItem("STARTER_PICKAXE", new ItemInfo("Starter Pickaxe", stats, ItemType.PICKAXE, Material.WOODEN_PICKAXE, Rarity.COMMON));

        stats = new Stats(true);
        stats.set(StatType.DAMAGE, 3500);
        ItemInfo errorScythe = new ItemInfo("Error Scythe", stats, ItemType.SWORD, Material.DIAMOND_HOE, Rarity.LEGENDARY);
        errorScythe.setAbility(abilityManager.getAbilityByID("ERROR_SCYTHE_ABILITY"));
        registerItem("ERROR_SCYTHE", errorScythe);
    }

    public ItemStack buildItem(String ID){
        return buildItem(getItemByID(ID));
    }

    public ItemStack buildItem(ItemInfo info){
        List<String> infoList = info.toList();
        String name = infoList.get(0);
        List<String> lore = infoList.subList(1, infoList.size());

        return ItemBuilder.from(info.getBaseItem()).setName(name).setLore(lore).unbreakable().setNbt("ITEM_ID", info.getID()).build();
    }

    public @Nullable ItemInfo getItemByID(String ID){
        return itemsMap.get(ID);
    }

    public @Nullable ItemInfo getItemFromItemStack(ItemStack itemStack){
        String ID = getIDofItemStack(itemStack);
        return getItemByID(ID);
    }

    @NotNull
    public Set<String> getKeys() {
        return itemsMap.keySet();
    }

    public String getIDofItemStack(ItemStack itemStack) {
        try {
            return ItemNbt.getString(itemStack, "ITEM_ID");
        } catch (NullPointerException nullPointerException) {
            return "";
        }
    }

    public static ItemsManager getInstance() {
        if (instance == null){
            instance = new ItemsManager();
        }
        return instance;
    }
}
