package me.lidan.cavecrawlers.api;

import me.lidan.cavecrawlers.items.ItemInfo;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * API for managing custom items in the CaveCrawlers plugin.
 * Provides methods for registering, retrieving, and building custom items, as well as extracting item information.
 */
public interface ItemsAPI {
    /**
     * Registers an item with the given ID and ItemInfo.
     *
     * @param ID   The unique identifier for the item.
     * @param info The ItemInfo associated with the item.
     */
    void registerItem(String ID, ItemInfo info);

    /**
     * Retrieves an ItemInfo object by its unique ID.
     *
     * @param ID The unique identifier for the item.
     * @return The ItemInfo associated with the ID, or null if not found.
     */
    @Nullable ItemInfo getItemByID(String ID);

    /**
     * Builds an ItemStack from the given ID and amount.
     *
     * @param ID     The unique identifier for the item.
     * @param amount The amount of the item to create.
     * @return An ItemStack representing the item.
     */
    ItemStack buildItem(String ID, int amount);

    /**
     * Builds an ItemStack from the given ItemInfo and amount.
     *
     * @param itemInfo The ItemInfo to create the ItemStack from.
     * @param amount   The amount of the item to create.
     * @return An ItemStack representing the item.
     */
    ItemStack buildItem(ItemInfo itemInfo, int amount);

    /**
     * Retrieves an ItemInfo from the given ItemStack.
     *
     * @param itemStack The ItemStack to extract the ItemInfo from.
     * @return The ItemInfo associated with the ItemStack, or null if not found.
     */
    @Nullable ItemInfo getItemFromItemStack(ItemStack itemStack);

    /**
     * Retrieves all items in the given inventory.
     *
     * @param inventory The inventory to check.
     * @return A map of ItemInfo and their respective amounts in the inventory.
     */
    Map<ItemInfo, Integer> getAllItems(Inventory inventory);

    /**
     * Gives multiple ItemStacks to a player.
     *
     * @param player The player to whom the items will be given.
     * @param items  The ItemStacks to give to the player.
     */
    void giveItemStacks(Player player, ItemStack... items);

    /**
     * Gives a specific amount of an item to a player.
     *
     * @param player   The player to whom the item will be given.
     * @param itemInfo The ItemInfo of the item to give.
     * @param amount   The amount of the item to give.
     */
    void giveItem(Player player, ItemInfo itemInfo, int amount);

    /**
     * Checks if a player has a specific amount of an item.
     *
     * @param player   The player to check.
     * @param material The ItemInfo of the item to check.
     * @param amount   The amount of the item to check for.
     * @return true if the player has the specified amount, false otherwise.
     */
    boolean hasItem(Player player, ItemInfo material, int amount);

    /**
     * Checks if a player has multiple items.
     *
     * @param player The player to check.
     * @param items  A map of ItemInfo and their respective amounts to check for.
     * @return true if the player has all specified items in the required amounts, false otherwise.
     */
    boolean hasItems(Player player, Map<ItemInfo, Integer> items);

    /**
     * Removes a specific amount of an item from a player's inventory.
     *
     * @param player   The player from whom the item will be removed.
     * @param material The ItemInfo of the item to remove.
     * @param amount   The amount of the item to remove.
     */
    void removeItems(Player player, ItemInfo material, int amount);

    /**
     * Removes an item from the registry by its unique ID.
     *
     * @param id The unique identifier of the item to remove.
     */
    void removeItem(String id);
}
