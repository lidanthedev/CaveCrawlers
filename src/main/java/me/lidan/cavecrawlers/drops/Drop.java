package me.lidan.cavecrawlers.drops;

import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.stats.Stat;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.utils.RandomUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Drop implements ConfigurationSerializable {
    private final ItemInfo itemInfo;
    private final int minAmount;
    private final int maxAmount;
    private final double chance;
    private final boolean announce;

    public Drop(ItemInfo itemInfo, int minAmount, int amount, double chance, boolean announce) {
        this.itemInfo = itemInfo;
        this.minAmount = minAmount;
        this.maxAmount = amount;
        this.chance = chance;
        this.announce = announce;
    }

    public Drop(String itemID, int minAmount, int maxAmount, double chance, boolean announce){
        this(ItemsManager.getInstance().getItemByID(itemID), minAmount, maxAmount, chance, announce);
    }

    public Drop(String itemID, int amount, double chance, boolean announce){
        this(ItemsManager.getInstance().getItemByID(itemID), amount, amount, chance, announce);
    }

    public void roll(Player player){
        double newDropChance = getNewDropChance(player);

        if (RandomUtils.chanceOf(newDropChance)){
            drop(player);
        }
    }

    private double getNewDropChance(Player player) {
        Stats stats = StatsManager.getInstance().getStats(player);
        Stat magicFind = stats.get(StatType.MAGIC_FIND);
        return chance * (1 + magicFind.getValue()/100);
    }

    public void sendDropMessage(Player player){
        DropRarity dropRarity = DropRarity.getRarity(chance);
        double newDropChance = getNewDropChance(player);
        String chanceStr = ChatColor.GRAY + " (" + chance + "%)";
        if (chance != newDropChance) {
            chanceStr += ChatColor.AQUA + " -> " + newDropChance + "%";
        }
        String message = dropRarity + itemInfo.getName() + chanceStr;

        player.sendMessage(message);
    }

    public void drop(Player player){
        if (announce)
            sendDropMessage(player);
        ItemsManager itemsManager = ItemsManager.getInstance();
        int amount = RandomUtils.randomInt(minAmount, maxAmount);
        itemsManager.giveItem(player, itemInfo, amount);
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("itemID", itemInfo.getID());
        map.put("amount", maxAmount);
        map.put("chance", chance);
        map.put("announce", announce);
        return map;
    }

    public static Drop deserialize(Map<String, Object> map){
        String itemID = (String) map.get("itemID");

        int minAmount = 0;
        int maxAmount = 0;

        String amountStr = map.get("amount").toString();
        if (amountStr.contains("-")){
            String[] arr = amountStr.split("-");
            minAmount = Integer.parseInt(arr[0]);
            maxAmount = Integer.parseInt(arr[1]);
        }
        else{
            minAmount = Integer.parseInt(amountStr);
            maxAmount = Integer.parseInt(amountStr);
        }

        double chance = (double) map.get("chance");

        boolean announce = (boolean) map.getOrDefault("announce", false);

        return new Drop(itemID, minAmount, maxAmount, chance, announce);
    }
}
