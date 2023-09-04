package me.lidan.cavecrawlers.items;

import lombok.Data;
import me.lidan.cavecrawlers.items.abilities.ItemAbility;
import org.bukkit.inventory.ItemStack;

@Data
public class ItemInfo {
    private String name;
    private String description;
    private ItemType type;
    private ItemStack baseItem;
    private Rarity rarity;
    private ItemAbility ability;


}
