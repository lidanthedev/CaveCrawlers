package me.lidan.cavecrawlers.items;


import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class ItemType {
    private static final Map<String, ItemType> itemTypes = new LinkedHashMap<>();
    public static final ItemType SHIELD = register("SHIELD", new ItemType("Shield", ItemSlot.OFF_HAND));
    public static final ItemType ALCHEMY_BAG = register("ALCHEMY_BAG", new ItemType("Alchemy Bag", ItemSlot.HAND));
    public static final ItemType MATERIAL = register("MATERIAL", new ItemType("Material", ItemSlot.HAND));
    public static final ItemType PICKAXE = register("PICKAXE", new ItemType("Pickaxe", ItemSlot.HAND));
    public static final ItemType DRILL = register("DRILL", new ItemType("Drill", ItemSlot.HAND));
    public static final ItemType WEAPON = register("WEAPON", new ItemType("Weapon", ItemSlot.HAND));
    public static final ItemType SWORD = register("SWORD", new ItemType("Sword", ItemSlot.HAND));
    public static final ItemType BOW = register("BOW", new ItemType("Bow", ItemSlot.HAND));
    public static final ItemType WAND = register("WAND", new ItemType("Wand", ItemSlot.HAND));
    public static final ItemType AXE = register("AXE", new ItemType("Axe", ItemSlot.HAND));
    public static final ItemType SHOVEL = register("SHOVEL", new ItemType("Shovel", ItemSlot.HAND));
    public static final ItemType TALISMAN = register("TALISMAN", new ItemType("Talisman", ItemSlot.OFF_HAND));
    public static final ItemType HELMET = register("HELMET", new ItemType("Helmet", ItemSlot.ARMOR));
    public static final ItemType CHESTPLATE = register("CHESTPLATE", new ItemType("Chestplate", ItemSlot.ARMOR));
    public static final ItemType LEGGINGS = register("LEGGINGS", new ItemType("Leggings", ItemSlot.ARMOR));
    public static final ItemType BOOTS = register("BOOTS", new ItemType("Boots", ItemSlot.ARMOR));
    public static final ItemType ARMOR = register("ARMOR", new ItemType("Armor", ItemSlot.ARMOR));
    public static final ItemType UNIQUE_ITEM = register("UNIQUE_ITEM", new ItemType("Unique Item", ItemSlot.HAND));
    public static final ItemType OFF_HAND = register("OFF_HAND", new ItemType("Off Hand", ItemSlot.OFF_HAND));
    public static final ItemType ACCESSORY = register("ACCESSORY", new ItemType("Accessory", ItemSlot.INVENTORY));
    public static final ItemType INVENTORY = register("INVENTORY", new ItemType("Inventory", ItemSlot.INVENTORY));
    public static final ItemType PET = register("PET", new ItemType("Pet", ItemSlot.OFF_HAND));
    public static final ItemType HOTBAR = register("HOTBAR", new ItemType("Hotbar", ItemSlot.HOTBAR));

    private String id;
    private final String name;
    private final ItemSlot slot;

    public ItemType(String name, ItemSlot slot) {
        this.name = name;
        this.slot = slot;
    }

    public static ItemType valueOf(String key) {
        ItemType itemType = itemTypes.get(key.toUpperCase());
        if (itemType == null) {
            throw new IllegalArgumentException("Item type " + key + " does not exist!");
        }
        return itemType;
    }

    public static ItemType register(String id, ItemType itemType) {
        id = id.toUpperCase();
        itemType.id = id;
        itemTypes.put(id, itemType);
        return itemType;
    }

    public static ItemType[] values() {
        return itemTypes.values().toArray(new ItemType[0]);
    }

    public String name() {
        return id;
    }

    public boolean isWeapon() {
        return this == WEAPON || this == SWORD || this == BOW || this == WAND;
    }
}
