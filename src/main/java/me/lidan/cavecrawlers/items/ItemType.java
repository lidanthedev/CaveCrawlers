package me.lidan.cavecrawlers.items;


import lombok.Getter;

@Getter
public enum ItemType {
    SHIELD("Shield", ItemSlot.HAND),
    ALCHEMY_BAG("Alchemy Bag", ItemSlot.HAND),
    MATERIAL("Material", ItemSlot.HAND),
    PICKAXE("Pickaxe", ItemSlot.HAND),
    DRILL("Drill", ItemSlot.HAND),
    SWORD("Sword", ItemSlot.HAND),
    BOW("Bow", ItemSlot.HAND),
    WAND("Wand", ItemSlot.HAND),
    AXE("Axe", ItemSlot.HAND),
    TALISMAN("Talisman", ItemSlot.OFF_HAND),
    HELMET("Helmet", ItemSlot.ARMOR),
    CHESTPLATE("Chestplate", ItemSlot.ARMOR),
    LEGGINGS("Leggings", ItemSlot.ARMOR),
    BOOTS("Boots", ItemSlot.ARMOR),
    ARMOR("Armor", ItemSlot.ARMOR),
    UNIQUE_ITEM("Unique Item", ItemSlot.HAND),

    ;

    private final String name;
    private final ItemSlot slot;

    ItemType(String name, ItemSlot slot) {
        this.name = name;
        this.slot = slot;
    }
}
