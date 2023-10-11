package me.lidan.cavecrawlers.items;

import lombok.Data;
import lombok.Setter;
import me.lidan.cavecrawlers.items.abilities.AbilityManager;
import me.lidan.cavecrawlers.items.abilities.ItemAbility;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ItemInfo implements ConfigurationSerializable {
    private String name;
    private String description;
    private Stats stats;
    private ItemType type;
    private ItemStack baseItem;
    private Rarity rarity;
    private String abilityID;
    private String ID;

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

        String name = (String)map.get("name");
        String description = (String)map.get("description");


        Map<String, Object> statsMap = (Map<String, Object>) map.get("stats");
        Stats stats = Stats.deserialize(statsMap);

        ItemType type = ItemType.valueOf((String)map.get("type"));
        Rarity rarity = Rarity.valueOf((String)map.get("rarity"));
        ItemStack itemStack = (ItemStack) map.get("baseItem");

        ItemAbility ability = null;
        String abilityID = (String)map.get("ability");

        return new ItemInfo(name, description, stats, type, itemStack, rarity, abilityID);

    }

}
