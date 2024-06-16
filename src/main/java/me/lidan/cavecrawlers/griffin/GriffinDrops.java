package me.lidan.cavecrawlers.griffin;

import lombok.Data;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@Data
public class GriffinDrops implements ConfigurationSerializable {
    private List<GriffinDrop> drops;

    public GriffinDrops(List<GriffinDrop> drops) {
        this.drops = drops;
    }

    public void drop(Player player){
        drop(player, player.getLocation());
    }

    public void drop(Player player, Location location){
        for (GriffinDrop drop : drops){
            if (drop.rollChance(player)){
                drop.drop(player, location);
                return;
            }
        }
    }


    @NotNull
    @Override
    public Map<String, Object> serialize() {
        return Map.of("drops", drops);
    }

    public static GriffinDrops deserialize(Map<String, Object> map) {
        return new GriffinDrops((List<GriffinDrop>) map.get("drops"));
    }
}
