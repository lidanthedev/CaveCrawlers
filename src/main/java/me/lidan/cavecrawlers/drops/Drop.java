package me.lidan.cavecrawlers.drops;

import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.stats.Stat;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.utils.RandomUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Drop implements ConfigurationSerializable {
    private final ItemInfo itemInfo;
    private final int amount;
    private final double chance;
    private final boolean announce;

    public Drop(ItemInfo itemInfo, int amount, double chance, boolean announce) {
        this.itemInfo = itemInfo;
        this.amount = amount;
        this.chance = chance;
        this.announce = announce;
    }

    public Drop(String itemID, int amount, double chance, boolean announce){
        this(ItemsManager.getInstance().getItemByID(itemID), amount, chance, announce);
    }

    public void roll(Player player){
        Stats stats = StatsManager.getInstance().getStats(player);
        Stat magicFind = stats.get(StatType.MAGIC_FIND);
        double newDropChance = chance * (1 + magicFind.getValue()/100);

        if (RandomUtils.chanceOf(newDropChance)){
            drop(player);
        }
    }

    public void sendDropMessage(Player player){
        DropRarity dropRarity = DropRarity.getRarity(chance);
        String message = dropRarity + itemInfo.getName() + ChatColor.GRAY + "(" + chance + "%)";

        player.sendMessage(message);
    }

    public void drop(Player player){
        DropRarity dropRarity = DropRarity.getRarity(chance);
        sendDropMessage(player);
        ItemsManager itemsManager = ItemsManager.getInstance();
        itemsManager.giveItem(player, itemInfo, amount);
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("itemID", itemInfo.getID());
        map.put("amount", amount);
        map.put("chance", chance);
        map.put("announce", announce);
        return map;
    }

    public static Drop deserialize(Map<String, Object> map){
        String itemID = (String) map.get("itemID");

        int amount = (int) map.get("amount");

        double chance = (double) map.get("chance");

        boolean announce = (boolean) map.get("announce");

        return new Drop(itemID, amount, chance, announce);
    }
}
