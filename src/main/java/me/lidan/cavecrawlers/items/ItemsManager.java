package me.lidan.cavecrawlers.items;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.util.ItemNbt;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.items.abilities.AbilityManager;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.utils.CustomConfig;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ItemsManager {
    public static final String ITEM_ID = "ITEM_ID";
    private static ItemsManager instance;
    private final Map<String, ItemInfo> itemsMap;
    private final ConfigurationSection vanillaConversion;
    private final CaveCrawlers plugin = CaveCrawlers.getInstance();

    public ItemsManager() {
        itemsMap = new HashMap<>();
        vanillaConversion = CaveCrawlers.getInstance().getConfig().getConfigurationSection("vanilla-convert");
    }

    public void registerItem(String ID, ItemInfo info){
        info.setID(ID);
        itemsMap.put(ID, info);
    }

    public void unregisterItem(String ID) {
        itemsMap.remove(ID);
    }

    public ItemStack buildItem(String ID, int amount){
        return buildItem(getItemByID(ID), amount);
    }

    public ItemStack buildItem(ItemInfo info, int amount){
        List<String> infoList = info.toList();
        if (infoList == null){
            return ItemBuilder.from(info.getBaseItem().clone()).amount(amount).build();
        }
        String name = infoList.get(0);
        List<String> lore = infoList.subList(1, infoList.size());

        return ItemBuilder
                .from(info.getBaseItem().clone())
                .setName(name)
                .setLore(lore)
                .unbreakable()
                .flags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_DYE)
                .setNbt(ITEM_ID, info.getID())
                .amount(amount)
                .build();
    }

    public @Nullable ItemInfo getItemByID(String ID){
        if (ID == null){
            return null;
        }

        if (ID.startsWith("VANILLA-")){
            Material material = Material.getMaterial(ID.replace("VANILLA-", ""));
            if (material == null){
                return null;
            }
            return new VanillaItemInfo(material);
        }

        ItemInfo itemInfo = itemsMap.get(ID);
        if (itemInfo == null && !ID.isEmpty()){
            return null;
        }
        return itemInfo;
    }

    public @Nullable ItemInfo getItemFromItemStackSafe(ItemStack itemStack){
        String ID = getIDofItemStack(itemStack);
        if (ID == null && itemStack != null){
            ItemMeta meta = itemStack.getItemMeta();
            if (meta == null){
                return null;
            }
            String displayName = meta.getDisplayName();
            displayName = ChatColor.stripColor(displayName);
            displayName = displayName.toUpperCase(Locale.ROOT);
            displayName = displayName.replace(" ", "_");
            ID = displayName;
        }
        return getItemByID(ID);
    }

    public @Nullable ItemInfo getItemFromItemStack(ItemStack itemStack){
        String ID = getIDofItemStack(itemStack);
        return getItemByID(ID);
    }

    @NotNull
    public Set<String> getKeys() {
        return itemsMap.keySet();
    }

    public String getIDofItemStackSafe(ItemStack itemStack) {
        String ID = getIDofItemStack(itemStack);
        if (ID == null)
            ID = "";
        return ID;
    }

    public @Nullable String getIDofItemStack(ItemStack itemStack) {
        try {
            String itemId = ItemNbt.getString(itemStack, ITEM_ID);
            if (itemId == null){
                return vanillaConversion.getString(itemStack.getType().name());
            }
            return itemId;
        } catch (NullPointerException nullPointerException) {
            return null;
        }
    }

    public ItemStack updateItemStack(ItemStack itemStack){
        if (itemStack == null) {
            return null;
        }
        ItemInfo itemInfo = getItemFromItemStack(itemStack);
        if (itemInfo != null){
            ItemStack builtItem = buildItem(itemInfo, itemStack.getAmount());
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta == null){
                return builtItem;
            }
            if (itemMeta.hasEnchants()){
                builtItem.addUnsafeEnchantments(itemMeta.getEnchants());
            }

            // preserve the custom nbt
            for (NamespacedKey key : itemMeta.getPersistentDataContainer().getKeys()) {
                if (key.getNamespace().equalsIgnoreCase(plugin.getName()) && key.getKey().equals(ITEM_ID)){
                    continue;
                }
                String value = itemMeta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                builtItem = ItemNbt.setString(builtItem, key.getKey(), value);
            }
            return builtItem;
        }
        return itemStack;
    }

    public void updatePlayerInventory(Player player){
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            contents[i] = updateItemStack(contents[i]);
        }
        player.getInventory().setContents(contents);
    }

    public Map<ItemInfo, Integer> getAllItems(Player player) {
        Map<ItemInfo, Integer> items = new HashMap<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            ItemInfo ID = getItemFromItemStack(item);
            items.put(ID, items.getOrDefault(ID, 0) + item.getAmount());
        }
        return items;
    }

    public Map<String, Integer> itemMapToStringMap(Map<ItemInfo, Integer> itemsMap){
        Map<String, Integer> itemIDmap = new HashMap<>();
        for (ItemInfo itemInfo : itemsMap.keySet()) {
            int amount = itemsMap.get(itemInfo);
            itemIDmap.put(itemInfo.getID(), amount);
        }
        return itemIDmap;
    }

    public Map<ItemInfo, Integer> stringMapToItemMap(Map<String, Integer> itemIdMap){
        Map<ItemInfo, Integer> itemsMap = new HashMap<>();

        for (String itemId : itemIdMap.keySet()) {
            ItemInfo itemInfo = getItemByID(itemId);
            int amount = itemIdMap.get(itemId);
            itemsMap.put(itemInfo, amount);
        }
        return itemsMap;
    }

    public void giveItemStacks(Player player, ItemStack... items){
        HashMap<Integer, ItemStack> dropItems = player.getInventory().addItem(items);
        Location location = player.getLocation();
        for (Integer i : dropItems.keySet()) {
            ItemStack itemStack = dropItems.get(i);
            location.getWorld().dropItem(location, itemStack);
        }
    }

    public void giveItem(Player player, ItemInfo itemInfo, int amount){
        giveItemStacks(player, buildItem(itemInfo, amount));
    }

    public void giveItems(Player player, Map<ItemInfo, Integer> items){
        for (ItemInfo material : items.keySet()) {
            int amount = items.get(material);
            giveItem(player, material, amount);
        }
    }

    public boolean hasItem(Player player, ItemInfo material, int amount) {
        Map<ItemInfo, Integer> inventory = getAllItems(player);
        return inventory.getOrDefault(material, 0) >= amount;
    }

    public boolean hasItems(Player player, Map<ItemInfo, Integer> items){
        for (ItemInfo material : items.keySet()) {
            if (!hasItem(player, material, items.get(material))){
                return false;
            }
        }
        return true;
    }

    public void removeItems(Player player,ItemInfo material, int amount) {
        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);

            if (item == null) continue;

            String ID = getIDofItemStackSafe(item);
            if (ID.equals(material.getID())) {
                if (item.getAmount() > amount) {
                    item.setAmount(item.getAmount() - amount);
                    player.getInventory().setItem(i, item);
                    break;
                } else {
                    amount -= item.getAmount();
                    player.getInventory().setItem(i, new ItemStack(Material.AIR));
                }
            }
        }
    }

    public void removeItems(Player player, Map<ItemInfo, Integer> items) {
        for (ItemInfo material : items.keySet()) {
            this.removeItems(player, material, items.get(material));
        }
    }

    public void clear(){
        itemsMap.clear();
    }

    public void setItem(String Id, ItemInfo itemInfo){
        ItemsLoader loader = ItemsLoader.getInstance();
        CustomConfig config = loader.getConfig(Id);
        config.set(Id, itemInfo);
        config.save();
        registerItem(Id, itemInfo);
    }

    public static ItemsManager getInstance() {
        if (instance == null){
            instance = new ItemsManager();
        }
        return instance;
    }
}
