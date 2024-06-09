package me.lidan.cavecrawlers.griffin;

import lombok.Data;
import lombok.ToString;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.utils.Range;
import me.lidan.cavecrawlers.utils.StringUtils;
import me.lidan.cavecrawlers.utils.VaultUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Data
@ToString(exclude = {"itemsManager", "griffinManager"})
public class GriffinDrop implements ConfigurationSerializable {
    private final ItemsManager itemsManager;
    private final GriffinManager griffinManager;
    private String type;
    private double chance;
    private String value;

    public GriffinDrop(String type, double chance, String value) {
        this.type = type;
        this.chance = chance;
        this.value = value;
        itemsManager = ItemsManager.getInstance();
        griffinManager = GriffinManager.getInstance();
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
                giveMob(location);
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
        String message = ChatColor.GOLD + ChatColor.BOLD.toString() + "GRIFFIN!" + ChatColor.GOLD + " you got %s coins!".formatted(StringUtils.getNumberFormat(amount));
        player.sendMessage(message);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
    }

    private void giveMob(Location location) {
        griffinManager.spawnMob(value, location);
    }

    private void giveItem(Player player) {
        if (value.contains(" ")){
            String[] split = value.split(" ");
            Range range = new Range(split[1]);
            itemsManager.giveItem(player, itemsManager.getItemByID(split[0]), range.getRandom());
        }
        else{
            itemsManager.giveItem(player, itemsManager.getItemByID(value), 1);
        }
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        return Map.of("type", type, "chance", chance, "value", value);
    }

    public static GriffinDrop deserialize(Map<String, Object> map) {
        return new GriffinDrop((String) map.get("type"), (double) map.get("chance"), (String) map.get("value"));
    }
}
