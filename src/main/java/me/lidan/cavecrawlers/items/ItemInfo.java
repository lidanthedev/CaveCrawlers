package me.lidan.cavecrawlers.items;

import lombok.Data;
import lombok.Setter;
import me.lidan.cavecrawlers.items.abilities.AbilityManager;
import me.lidan.cavecrawlers.items.abilities.ItemAbility;
import me.lidan.cavecrawlers.stats.Stats;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Data
public class ItemInfo {
    private String name;
    private String description;
    private Stats stats;
    private ItemType type;
    private ItemStack baseItem;
    private Rarity rarity;
    private ItemAbility ability;
    private String ID;

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

    public List<String> toList(){
        List<String> list = new ArrayList<>();
        list.add(rarity.getColor() + name);
        list.add(ChatColor.DARK_GRAY + type.getName());
        list.add("");
        list.addAll(stats.toLoreList());
        list.add("");
        if (description != null){
            list.add(description);
            list.add("");
        }
        if (ability != null){
            list.addAll(ability.toList());
            list.add("");
        }
        list.add(rarity.getColor().toString() + ChatColor.BOLD + rarity.name());
        return list;
    }

}
