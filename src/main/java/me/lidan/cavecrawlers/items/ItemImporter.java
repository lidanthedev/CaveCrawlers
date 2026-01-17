package me.lidan.cavecrawlers.items;

import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ItemImporter {

    private ItemStack item;
    private ItemMeta meta;

    public ItemImporter(ItemStack item) {
        this.item = item;
        this.meta = item.getItemMeta();
        if (this.meta == null){
            throw new IllegalArgumentException("Item missing meta");
        }
        if (!meta.hasDisplayName()){
            throw new IllegalArgumentException("Item missing name");
        }
        if (!meta.hasLore()){
            throw new IllegalArgumentException("Item missing lore");
        }
    }

    public List<String> toList(){
        ItemMeta meta = item.getItemMeta();
        List<String> list = new ArrayList<>();
        list.add(meta.getDisplayName());
        list.addAll(meta.getLore());
        return list;
    }

    public ItemStack toBaseItem(){
        ItemStack base = item.clone();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(null);
        meta.setLore(null);
        base.setItemMeta(meta);
        base.setAmount(1);
        return base;
    }

    public ItemInfo toItemInfo(){
        List<String> list = toList();
        String name = meta.getDisplayName();
        String typeStr = list.get(1);
        typeStr = ChatColor.stripColor(typeStr);
        typeStr = typeStr.toUpperCase(Locale.ROOT);
        typeStr = typeStr.replace(' ','_');
        ItemType type = getItemType(typeStr);
        Stats stats = toStats(list.subList(3, list.size() - 1));
        ItemStack baseItem = toBaseItem();
        String rarityLine = list.get(list.size() - 1);
        rarityLine = ChatColor.stripColor(rarityLine);
        Rarity rarity = Rarity.valueOf(rarityLine);
        return new ItemInfo(name, stats, type, baseItem, rarity);
    }

    public ItemType getItemType(String typeStr){
        try{
            return ItemType.valueOf(typeStr);
        }
        catch (IllegalArgumentException e){
            for (ItemType value : ItemType.values()) {
                String typeName = value.getName().toUpperCase(Locale.ROOT);
                if (typeStr.equals(typeName)){
                    return value;
                }
            }
            throw new IllegalArgumentException("Item type " + typeStr + " doesn't exist!");
        }
    }

    public Stats toStats(List<String> statLines) {
        Stats stats = new Stats();
        for (String line : statLines) {
            line = ChatColor.stripColor(line);
            // Health: +500
            String[] args = line.split(": \\+");
            for (StatType statType : StatType.values()) {
                String statTypeLine = statType.getName();
                String statLineOnLore = args[0];
                statLineOnLore = statLineOnLore.replace("Defence", "Defense");
                if (statLineOnLore.equals(statTypeLine)){
                    double value = Double.parseDouble(args[1]);
                    stats.set(statType, value);
                }
            }
        }
        return stats;

    }
}
