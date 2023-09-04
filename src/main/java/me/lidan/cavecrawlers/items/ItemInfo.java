package me.lidan.cavecrawlers.items;

import lombok.Data;
import me.lidan.cavecrawlers.items.abilities.AbilityManager;
import me.lidan.cavecrawlers.items.abilities.ItemAbility;
import me.lidan.cavecrawlers.stats.Stats;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Data
public class ItemInfo {
    private String name;
    private String description;
    private Stats stats;
    private ItemType type;
    private ItemStack baseItem;
    private Rarity rarity;
    private ItemAbility ability;

    public ItemInfo(String name,  Stats stats, ItemType type, Material material, Rarity rarity) {
        this(name, stats, type, new ItemStack(material), rarity);
    }

    public ItemInfo(String name,  Stats stats, ItemType type, ItemStack baseItem, Rarity rarity) {
        this(name, null, stats, type, baseItem, rarity, null);
    }

    public ItemInfo(String name, String description, Stats stats, ItemType type, ItemStack baseItem, Rarity rarity, ItemAbility ability) {
        this.name = name;
        this.description = description;
        this.stats = stats;
        this.type = type;
        this.baseItem = baseItem;
        this.rarity = rarity;
        this.ability = ability;
    }

}
