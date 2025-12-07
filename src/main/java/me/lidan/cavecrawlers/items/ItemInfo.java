package me.lidan.cavecrawlers.items;

import lombok.Data;
import me.lidan.cavecrawlers.items.abilities.AbilityManager;
import me.lidan.cavecrawlers.items.abilities.ItemAbility;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ItemInfo implements ConfigurationSerializable, Cloneable {
    private String name;
    private String description;
    private Stats stats;
    private ItemType type;
    private ItemStack baseItem;
    private Rarity rarity;
    private String abilityID;
    private String ID;
    private boolean fullyLoaded = true;

    public ItemInfo(String name,  Stats stats, ItemType type, Material material, Rarity rarity) {
        this(name, stats, type, new ItemStack(material), rarity);
    }

    public ItemInfo(String name,  Stats stats, ItemType type, ItemStack baseItem, Rarity rarity) {
        this(name, null, stats, type, baseItem, rarity, null);
    }

    public ItemInfo(String name, String description, Stats stats, ItemType type, ItemStack baseItem, Rarity rarity, String abilityID) {
        this.name = name;
        this.description = description;
        this.stats = stats;
        this.type = type;
        this.baseItem = baseItem;
        this.rarity = rarity;
        this.abilityID = abilityID;
    }

    public List<String> toList(){
        List<String> list = new ArrayList<>();
        list.add(rarity.getColor() + name);
        list.add(ChatColor.DARK_GRAY + type.getName());
        list.add("");
        List<String> loreList = stats.toLoreList();
        if (!loreList.isEmpty()) {
            list.addAll(loreList);
            list.add("");
        }
        if (description != null){
            list.addAll(StringUtils.loreBuilder(description));
            list.add("");
        }
        ItemAbility ability = getAbility();
        if (ability != null && !ability.toList().isEmpty()) {
            list.addAll(ability.toList());
            list.add("");
        }
        list.add(rarity.getColor().toString() + ChatColor.BOLD + rarity.name());
        return list;
    }

    public ItemAbility getAbility(){
        return AbilityManager.getInstance().getAbilityByID(abilityID);
    }

    public String getFormattedName(){
        return rarity.getColor() + name;
    }

    public String getFormattedNameWithAmount(int amount) {
        return getFormattedName() + ChatColor.DARK_GRAY + " x" + amount;
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("description", description);
        map.put("stats", stats.serialize());
        map.put("type", type.name());
        map.put("baseItem", baseItem);
        map.put("rarity", rarity.name());
        map.put("ability", abilityID);
        return map;
    }

    public static ItemInfo deserialize(Map<String, Object> map) {
        boolean fullyLoaded = true;
        String name = (String)map.get("name");
        String description = (String)map.get("description");

        Map<String, Object> statsMap = (Map<String, Object>) map.get("stats");
        Stats stats = new Stats();
        try {
            stats = Stats.deserialize(statsMap);
        } catch (IllegalArgumentException e) {
            fullyLoaded = false;
        }

        ItemType type = ItemType.valueOf((String)map.get("type"));
        Rarity rarity = Rarity.valueOf((String)map.get("rarity"));
        ItemStack itemStack = (ItemStack) map.get("baseItem");

        String abilityID = (String)map.get("ability");

        ItemInfo itemInfo = new ItemInfo(name, description, stats, type, itemStack, rarity, abilityID);
        itemInfo.fullyLoaded = fullyLoaded;
        return itemInfo;
    }

    @Override
    public ItemInfo clone() {
        try {
            ItemInfo clone = (ItemInfo) super.clone();
            clone.stats = this.stats.clone();
            clone.abilityID = this.abilityID;
            clone.baseItem = this.baseItem.clone();
            clone.name = this.name;
            clone.description = this.description;
            clone.type = this.type;
            clone.rarity = this.rarity;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
