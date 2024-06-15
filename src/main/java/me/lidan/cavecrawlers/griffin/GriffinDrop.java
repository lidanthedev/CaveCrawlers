package me.lidan.cavecrawlers.griffin;

import lombok.*;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.SimpleDrop;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import me.lidan.cavecrawlers.utils.Range;
import me.lidan.cavecrawlers.utils.StringUtils;
import me.lidan.cavecrawlers.utils.VaultUtils;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(exclude = {"itemsManager", "griffinManager"})
public class GriffinDrop extends Drop implements ConfigurationSerializable {
    private final ConfigMessage COINS_MESSAGE = ConfigMessage.getMessageOrDefault("griffin_coins_message", "&e&lGRIFFIN! you got %amount% coins!");
    private final ItemsManager itemsManager = ItemsManager.getInstance();
    private final GriffinManager griffinManager = GriffinManager.getInstance();

    public GriffinDrop(String type, double chance, String value, ConfigMessage announce) {
        super(type, chance, value, announce);
    }

    public GriffinDrop(String type, double chance, String value) {
        this(type, chance, value, null);
    }

    public void drop(Player player){
        drop(player, player.getLocation());
    }

    public void drop(Player player, Location location){
        switch (type){
            case "item":
                giveItem(player);
                break;
            case "mob":
                giveMob(player, location);
                break;
            case "coins":
                giveCoins(player);
                break;
        }
    }

    private void giveCoins(Player player) {
        Range range = new Range(value);
        int amount = range.getRandom();
        VaultUtils.giveCoins(player, amount);
        Map<String, String> placeholders = Map.of("amount", StringUtils.getNumberFormat(amount));
        COINS_MESSAGE.sendMessage(player, placeholders);
    }

    private void giveMob(Player player, Location location) {
        griffinManager.spawnMob(value, location, player);
    }

    private void giveItem(Player player) {
        int amount = 1;
        String itemID = value;
        if (value.contains(" ")){
            String[] split = value.split(" ");
            itemID = split[0];
            Range range = new Range(split[1]);
            amount = range.getRandom();
        }
        SimpleDrop drop = new SimpleDrop(itemID, amount, chance * 100, true);
        drop.drop(player);
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        return Map.of("type", type, "chance", chance, "value", value);
    }

    public static GriffinDrop deserialize(Map<String, Object> map) {
        return new GriffinDrop((String) map.get("type"), (double) map.get("chance"), (String) map.get("value"), ConfigMessage.getMessage((String) map.get("announce")));
    }
}
