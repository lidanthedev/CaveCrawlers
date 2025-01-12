package me.lidan.cavecrawlers.drops;

import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import lombok.Data;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import me.lidan.cavecrawlers.stats.Stat;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.utils.RandomUtils;
import me.lidan.cavecrawlers.utils.Range;
import me.lidan.cavecrawlers.utils.StringUtils;
import me.lidan.cavecrawlers.utils.VaultUtils;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Data
public class Drop implements ConfigurationSerializable {
    public record ItemDropInfo(int amount, ItemInfo itemInfo) {
    }

    private static final Logger log = LoggerFactory.getLogger(Drop.class);
    private static final ItemsManager itemsManager = ItemsManager.getInstance();
    private static final CaveCrawlers plugin = CaveCrawlers.getInstance();
    public static final ConfigMessage RARE_DROP_MESSAGE = ConfigMessage.getMessageOrDefault("rare_drop_message", "%dropRarity% %name%");
    private static final StatsManager statsManager = StatsManager.getInstance();

    protected DropType type;
    protected double chance;
    protected String value;
    protected @Nullable ConfigMessage announce; // config message for announcing the drop
    protected @Nullable StatType chanceModifier;
    protected @Nullable StatType amountModifier;

    protected Map<String, String> placeholders = new HashMap<>();

    public Drop(DropType type, double chance, String value, @Nullable ConfigMessage announce, @Nullable StatType chanceModifier, @Nullable StatType amountModifier) {
        this.type = type;
        this.chance = chance;
        this.value = value;
        this.announce = announce;
        this.chanceModifier = chanceModifier;
        this.amountModifier = amountModifier;
    }

    public Drop(DropType type, double chance, String value, @Nullable ConfigMessage announce) {
        this(type, chance, value, announce, null, null);
    }

    public Drop(String type, double chance, String value, @Nullable ConfigMessage announce) {
        this(DropType.valueOf(type.toUpperCase(Locale.ROOT)), chance, value, announce);
    }

    public Drop(String type, double chance, String value) {
        this(type, chance, value, null);
    }

    public void roll(Player player) {
        if (rollChance(player)) {
            drop(player);
        }
    }

    private double getNewDropChance(Player player) {
        if (chanceModifier == null){
            return chance;
        }
        Stats stats = statsManager.getStats(player);
        Stat magicFind = stats.get(chanceModifier);
        return chance * (1 + magicFind.getValue()/100);
    }

    private int getNewAmount(Player player, int amount) {
        if (amountModifier == null){
            return amount;
        }
        Stats stats = statsManager.getStats(player);
        double value = stats.get(amountModifier).getValue();
        int multi = 1 + (int) value/100;
        int remain = (int) (value % 100);
        if (RandomUtils.chanceOf(remain)){
            multi++;
        }
        amount *= multi;
        return amount;
    }

    public boolean rollChance(Player player) {
        return RandomUtils.chanceOf(getNewDropChance(player));
    }

    public void drop(Player player){
        drop(player, player.getLocation());
    }

    public void drop(Player player, Location location){
        if (announce != null){
            placeholders.clear();
            placeholders.put("player", player.getName());
            placeholders.put("chance", StringUtils.getNumberFormat(chance));
            placeholders.put("newChance", StringUtils.getNumberFormat(getNewDropChance(player)));
        }

        switch (type){
            case ITEM:
                giveItem(player);
                break;
            case MOB:
                giveMob(player, location);
                break;
            case COINS:
                giveCoins(player);
                break;
        }
    }

    protected void giveItem(Player player) {
        ItemDropInfo result = getItemDropInfo(value);
        if (result == null) return;
        int amount = getNewAmount(player, result.amount());
        itemsManager.giveItem(player, result.itemInfo(), amount);
        if (announce != null) {
            DropRarity dropRarity = DropRarity.getRarity(chance);
            placeholders.putAll(Map.of("amount", StringUtils.getNumberFormat(amount),
                    "name", result.itemInfo().getFormattedName(),
                    "rarity", result.itemInfo().getRarity().toString(),
                    "dropRarity", dropRarity.toString()));
            sendAnnounceMessage(player);
        }
    }

    /**
     * Parse the value from the config to get the item drop info
     * Format: [itemID] [amount]
     *
     * @param value the value to parse
     * @return the item drop info
     */
    public static @Nullable ItemDropInfo getItemDropInfo(String value) {
        int amount = 1;
        String itemID = value;
        if (value.contains(" ")){
            String[] split = value.split(" ");
            itemID = split[0];
            Range range = new Range(split[1]);
            amount = range.getRandom();
        }
        ItemInfo itemInfo = itemsManager.getItemByID(itemID);
        if (itemInfo == null){
            log.error("Item with ID {} not found", itemID);
            return null;
        }
        return new ItemDropInfo(amount, itemInfo);
    }

    protected void sendAnnounceMessage(Player player) {
        announce.sendMessage(player, placeholders);
    }

    protected Entity giveMob(Player player, Location location) {
        try {
            location = location.clone().add(0.5, 0, 0.5);
            Entity entity = plugin.getMythicBukkit().getAPIHelper().spawnMythicMob(value, location);

            if (announce != null) {
                placeholders.put("name", entity.getName());
                sendAnnounceMessage(player);
            }

            return entity;
        } catch (InvalidMobTypeException e) {
            log.error("Failed to spawn mobs", e);
        }
        return null;
    }

    protected void giveCoins(Player player) {
        Range range = new Range(value);
        int amount = range.getRandom();
        amount = getNewAmount(player, amount);
        VaultUtils.giveCoins(player, amount);
        if (announce != null) {
            placeholders.put("amount", StringUtils.getNumberFormat(amount));
            sendAnnounceMessage(player);
        }
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        return Map.of(
                "type", type,
                "chance", chance,
                "value", value,
                "announce", ConfigMessage.getIdOfMessage(announce),
                "chanceModifier", chanceModifier,
                "amountModifier", amountModifier
        );
    }

    public static Drop deserialize(Map<String, Object> map) {
        if (map.containsKey("itemID")){
            // legacy support
            String itemID = (String) map.get("itemID");
            String amountStr = map.get("amount").toString();
            ConfigMessage announce = null;
            if (map.getOrDefault("announce", false).equals(true)){
                announce = RARE_DROP_MESSAGE;
            }
            return new Drop(DropType.ITEM, (double) map.get("chance"), itemID + " " + amountStr, announce, StatType.MAGIC_FIND, null);
        }

        return new Drop(
                DropType.valueOf(((String) map.get("type")).toUpperCase(Locale.ROOT)),
                (double) map.get("chance"),
                (String) map.get("value"),
                ConfigMessage.getMessage((String) map.get("announce")),
                map.containsKey("chanceModifier") ? StatType.valueOf((String) map.get("chanceModifier")) : null,
                map.containsKey("amountModifier") ? StatType.valueOf((String) map.get("amountModifier")) : null
        );
    }
}
