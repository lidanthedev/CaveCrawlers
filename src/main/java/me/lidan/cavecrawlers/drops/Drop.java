package me.lidan.cavecrawlers.drops;

import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import lombok.Data;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.objects.ConfigMessage;
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
    private static final Logger log = LoggerFactory.getLogger(Drop.class);
    private static final ItemsManager itemsManager = ItemsManager.getInstance();
    private static final CaveCrawlers plugin = CaveCrawlers.getInstance();
    public static final ConfigMessage RARE_DROP_MESSAGE = ConfigMessage.getMessageOrDefault("rare_drop_message", "%dropRarity% %name%");
    protected DropType type;
    protected double chance;
    protected String value;
    protected @Nullable ConfigMessage announce; // config message for announcing the drop
    protected Map<String, String> placeholders = new HashMap<>();

    public Drop(DropType type, double chance, String value, @Nullable ConfigMessage announce) {
        this.type = type;
        this.chance = chance;
        this.value = value;
        this.announce = announce;
    }

    public Drop(String type, double chance, String value, @Nullable ConfigMessage announce) {
        this(DropType.valueOf(type.toUpperCase(Locale.ROOT)), chance, value, announce);
    }

    public Drop(String type, double chance, String value) {
        this(type, chance, value, null);
    }

    public void roll(Player player) {
        if (Math.random() * 100 <= chance) {
            drop(player);
        }
    }

    public void drop(Player player){
        drop(player, player.getLocation());
    }

    public void drop(Player player, Location location){
        if (announce != null){
            placeholders.clear();
            placeholders.put("player", player.getName());
            placeholders.put("chance", StringUtils.getNumberFormat(chance));
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
            return;
        }
        itemsManager.giveItem(player, itemInfo, amount);
        if (announce != null) {
            DropRarity dropRarity = DropRarity.getRarity(chance);
            Map<String, String> placeholders = Map.of("amount", StringUtils.getNumberFormat(amount),
                    "name", itemInfo.getFormattedName(),
                    "rarity", itemInfo.getRarity().toString(),
                    "dropRarity", dropRarity.toString());
            announce.sendMessage(player, placeholders);
        }
    }

    protected Entity giveMob(Player player, Location location) {
        try {
            Entity entity = plugin.getMythicBukkit().getAPIHelper().spawnMythicMob(value, location);

            if (announce != null) {
                Map<String, String> placeholders = Map.of("name", entity.getName());
                announce.sendMessage(player, placeholders);
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
        VaultUtils.giveCoins(player, amount);
        if (announce != null) {
            Map<String, String> placeholders = Map.of("amount", StringUtils.getNumberFormat(amount));
            announce.sendMessage(player, placeholders);
        }
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        return Map.of(
                "type", type,
                "chance", chance,
                "value", value,
                "announce", announce
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
            return new Drop("item", (double) map.get("chance"), itemID + " " + amountStr, announce);
        }

        return new Drop(
                (String) map.get("type"),
                (double) map.get("chance"),
                (String) map.get("value"),
                ConfigMessage.getMessage((String) map.get("announce"))
        );
    }
}
