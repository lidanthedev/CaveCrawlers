package me.lidan.cavecrawlers.items;


import lombok.Getter;

@Getter
public enum ItemType {
    MATERIAL("Material", ItemSlot.HAND),
    PICKAXE("Pickaxe", ItemSlot.HAND),
    SWORD("Sword", ItemSlot.HAND),
    BOW("Bow", ItemSlot.HAND),
    HELMET("Helmet", ItemSlot.ARMOR),
    CHESTPLATE("Chestplate", ItemSlot.ARMOR),
    LEGGINGS("Leggings", ItemSlot.ARMOR),
    BOOTS("Boots", ItemSlot.ARMOR);

    private final String name;
    private final ItemSlot slot;

    ItemType(String name, ItemSlot slot) {
        this.name = name;
        this.slot = slot;
    }
}
