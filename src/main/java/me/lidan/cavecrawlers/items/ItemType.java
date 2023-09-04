package me.lidan.cavecrawlers.items;


import lombok.Getter;

@Getter
public enum ItemType {
    MATERIAL("Material"),
    PICKAXE("Pickaxe"),
    SWORD("Sword"),
    BOW("Bow"),
    HELMET("Helmet"),
    CHESTPLATE("Chestplate"),
    LEGGINGS("Leggings"),
    BOOTS("Boots");

    private final String name;

    ItemType(String name) {
        this.name = name;
    }
}
