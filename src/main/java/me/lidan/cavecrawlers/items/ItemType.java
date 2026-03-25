package me.lidan.cavecrawlers.items;


import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class ItemType {
    public static final ItemType SHIELD = new ItemType("Shield", ItemSlot.OFF_HAND);
    public static final ItemType ALCHEMY_BAG = new ItemType("Alchemy Bag", ItemSlot.HAND);
    public static final ItemType MATERIAL = new ItemType("Material", ItemSlot.HAND);
    public static final ItemType PICKAXE = new ItemType("Pickaxe", ItemSlot.HAND);
    public static final ItemType DRILL = new ItemType("Drill", ItemSlot.HAND);
    public static final ItemType WEAPON = new ItemType("Weapon", ItemSlot.HAND);
    public static final ItemType SWORD = new ItemType("Sword", ItemSlot.HAND);
    public static final ItemType BOW = new ItemType("Bow", ItemSlot.HAND);
    public static final ItemType WAND = new ItemType("Wand", ItemSlot.HAND);
    public static final ItemType AXE = new ItemType("Axe", ItemSlot.HAND);
    public static final ItemType SHOVEL = new ItemType("Shovel", ItemSlot.HAND);
    public static final ItemType TALISMAN = new ItemType("Talisman", ItemSlot.OFF_HAND);
    public static final ItemType HELMET = new ItemType("Helmet", ItemSlot.ARMOR);
    public static final ItemType CHESTPLATE = new ItemType("Chestplate", ItemSlot.ARMOR);
    public static final ItemType LEGGINGS = new ItemType("Leggings", ItemSlot.ARMOR);
    public static final ItemType BOOTS = new ItemType("Boots", ItemSlot.ARMOR);
    public static final ItemType ARMOR = new ItemType("Armor", ItemSlot.ARMOR);
    public static final ItemType UNIQUE_ITEM = new ItemType("Unique Item", ItemSlot.HAND);
    public static final ItemType OFF_HAND = new ItemType("Off Hand", ItemSlot.OFF_HAND);
    public static final ItemType ACCESSORY = new ItemType("Accessory", ItemSlot.INVENTORY);
    public static final ItemType INVENTORY = new ItemType("Inventory", ItemSlot.INVENTORY);
    public static final ItemType PET = new ItemType("Pet", ItemSlot.OFF_HAND);
    public static final ItemType HOTBAR = new ItemType("Hotbar", ItemSlot.HOTBAR);
    private static final Map<String, ItemType> itemTypes = new LinkedHashMap<>();

    static {
        register("SHIELD", SHIELD);
        register("ALCHEMY_BAG", ALCHEMY_BAG);
        register("MATERIAL", MATERIAL);
        register("PICKAXE", PICKAXE);
        register("DRILL", DRILL);
        register("WEAPON", WEAPON);
        register("SWORD", SWORD);
        register("BOW", BOW);
        register("WAND", WAND);
        register("AXE", AXE);
        register("SHOVEL", SHOVEL);
        register("TALISMAN", TALISMAN);
        register("HELMET", HELMET);
        register("CHESTPLATE", CHESTPLATE);
        register("LEGGINGS", LEGGINGS);
        register("BOOTS", BOOTS);
        register("ARMOR", ARMOR);
        register("UNIQUE_ITEM", UNIQUE_ITEM);
        register("OFF_HAND", OFF_HAND);
        register("ACCESSORY", ACCESSORY);
        register("INVENTORY", INVENTORY);
        register("PET", PET);
        register("HOTBAR", HOTBAR);
    }

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

    public static void register(String id, ItemType itemType) {
        id = id.toUpperCase();
        itemType.id = id;
        itemTypes.put(id, itemType);
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
