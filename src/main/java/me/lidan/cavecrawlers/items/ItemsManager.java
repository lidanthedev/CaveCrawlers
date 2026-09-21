package me.lidan.cavecrawlers.items;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.util.ItemNbt;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.api.ItemsAPI;
import me.lidan.cavecrawlers.utils.BoostedCustomConfig;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class ItemsManager implements ItemsAPI {
    public static final String ITEM_ID = "ITEM_ID";
    public static final String NO_UPDATE = "NO_UPDATE";
    private static ItemsManager instance;
    private final Map<String, ItemInfo> itemsMap;
    private final CaveCrawlers plugin = CaveCrawlers.getInstance();

    private @Nullable ConfigurationSection getVanillaConversion() {
        return plugin.getConfig().getConfigurationSection("vanilla-convert");
    }

    private ItemsManager() {
        itemsMap = new HashMap<>();
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

    private static @NonNull ItemStack buildItemRaw(ItemInfo info, int amount) {
        return buildItemRaw(info, amount, info.toList(), info.getBaseItem().clone());
    }

    private static @NonNull ItemStack buildItemRaw(ItemInfo info, int amount, List<String> infoList, ItemStack clonedBaseItem) {
        if (infoList == null) {
            return ItemBuilder.from(clonedBaseItem).amount(amount).build();
        }
        String name = infoList.get(0);
        List<String> lore = infoList.subList(1, infoList.size());
        if (clonedBaseItem.getType() == Material.AIR || clonedBaseItem.getItemMeta() == null) {
            clonedBaseItem = new ItemStack(Material.PAPER);
            lore.add(0, ChatColor.RED + "base item is missing");
        }

        ItemStack builtItem = ItemBuilder
                .from(clonedBaseItem)
                .setName(name)
                .setLore(lore)
                .unbreakable()
                .flags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_DYE)
                .setNbt(ITEM_ID, info.getID())
                .amount(amount)
                .build();
        return builtItem;
    }

    public ItemStack buildItem(ItemInfo info, int amount){
        ItemStack originalItem = info.getBaseItem().clone();
        ItemStack builtItem = buildItemRaw(info, amount);
        ItemBuildEvent event = new ItemBuildEvent(originalItem, builtItem, info);
        Bukkit.getPluginManager().callEvent(event);
        return event.getBuiltItem();
    }

    public @Nullable ItemInfo getItemByID(String ID){
        if (ID == null){
            return null;
        }

        if (ID.startsWith("VANILLA-")){
            Material material = Material.getMaterial(ID.substring("VANILLA-".length()));
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

    public @Nullable ItemInfo reloadItemByID(String ID) {
        ItemsLoader loader = ItemsLoader.getInstance();
        BoostedCustomConfig config = loader.getConfig(ID);
        config.load();
        loader.registerItemsFromConfig(config);
        return itemsMap.get(ID);
    }

    /**
     * Resolves an item stack by its canonical metadata ID or configured vanilla conversion.
     * The method name is retained for API compatibility; display names are not item identity.
     */
    public @Nullable ItemInfo getItemFromItemStackSafe(ItemStack itemStack){
        return getItemFromItemStack(itemStack);
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
            String itemId = itemStack.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, ITEM_ID), PersistentDataType.STRING);
            if (itemId == null){
                ConfigurationSection vanillaConversion = getVanillaConversion();
                return vanillaConversion == null ? null : vanillaConversion.getString(itemStack.getType().name());
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
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, NO_UPDATE))) {
            return itemStack;
        }
        ItemInfo itemInfo = getItemFromItemStack(itemStack);
        if (itemInfo != null){
            ItemStack builtItem = buildItemRaw(itemInfo, itemStack.getAmount());
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta != null) {
                if (itemMeta.hasEnchants()){
                    builtItem.addUnsafeEnchantments(itemMeta.getEnchants());
                }
                try {
                    ItemMeta builtItemMeta = builtItem.getItemMeta();
                    if (builtItemMeta != null) {
                        itemMeta.getPersistentDataContainer().copyTo(builtItemMeta.getPersistentDataContainer(), true);
                        builtItem.setItemMeta(builtItemMeta);
                    }
                } catch (Exception ignored) {
                    // kept for 1.19 compatibility
                    // WARNING: only copies string nbt values
                    // preserve the custom nbt
                    for (NamespacedKey key : itemMeta.getPersistentDataContainer().getKeys()) {
                        if (key.getNamespace().equalsIgnoreCase(plugin.getName()) && key.getKey().equals(ITEM_ID)) {
                            continue;
                        }
                        if (!itemMeta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                            continue;
                        }
                        String value = itemMeta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                        if (value != null) {
                            builtItem = ItemNbt.setString(builtItem, key.getKey(), value);
                        }
                    }
                }
            }
            ItemUpdateEvent event = new ItemUpdateEvent(itemStack, builtItem, itemInfo);
            Bukkit.getPluginManager().callEvent(event);
            return event.getBuiltItem();
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
        return getAllItems(player.getInventory());
    }

    public Map<ItemInfo, Integer> getAllItems(Inventory inventory) {
        Map<ItemInfo, Integer> items = new HashMap<>();
        for (ItemStack item : inventory.getStorageContents()) {
            if (item == null) continue;
            ItemInfo ID = getItemFromItemStack(item);
            if (ID == null) continue;
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
        Map<ItemInfo, Integer> itemsMap = new LinkedHashMap<>();

        for (String itemId : itemIdMap.keySet()) {
            ItemInfo itemInfo = getItemByID(itemId);
            int amount = itemIdMap.get(itemId);
            if (itemInfo != null) {
                itemsMap.put(itemInfo, amount);
            }
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

    @Override
    public void removeItem(String id) {
        unregisterItem(id);
        ItemsLoader loader = ItemsLoader.getInstance();
        loader.remove(id);
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
        BoostedCustomConfig config = loader.getConfig(Id);
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

    public void loadNotFullyLoadedItems() {
        ItemsLoader loader = ItemsLoader.getInstance();
        List<String> toRemove = new ArrayList<>();
        for (String key : loader.getNotFullyLoadedItems().keySet()) {
            ItemInfo itemInfo = reloadItemByID(key);
            if (itemInfo != null && itemInfo.isFullyLoaded()) {
                toRemove.add(key);
            }
        }
        for (String key : toRemove) {
            loader.getNotFullyLoadedItems().remove(key);
        }
    }
}
