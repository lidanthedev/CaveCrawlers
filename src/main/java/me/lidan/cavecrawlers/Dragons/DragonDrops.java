package me.lidan.cavecrawlers.Dragons;

import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class DragonDrops implements ConfigurationSerializable {
    private List<DragonDrop> drops;

    public DragonDrops(List<DragonDrop> drops) {
        this.drops = drops;
    }

    public void drop(Player player){
        drop(player, player.getLocation());
    }

    public void drop(Player player, Location location){
        for (DragonDrop drop : drops){
            if (drop.rollChance(player)){
                drop.drop(player, location);
                return;
            }
        }
    }


    @NotNull
    @Override
    public Map<String, Object> serialize() {
        return Map.of("dragons", drops);
    }

    public static DragonDrops deserialize(Map<String, Object> map) {
        return new DragonDrops((List<DragonDrop>) map.get("dragons"));
    }
}