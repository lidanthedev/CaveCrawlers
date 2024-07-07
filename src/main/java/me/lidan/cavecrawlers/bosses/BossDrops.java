package me.lidan.cavecrawlers.bosses;

import me.lidan.cavecrawlers.griffin.GriffinDrop;
import me.lidan.cavecrawlers.griffin.GriffinDrops;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class BossDrops implements ConfigurationSerializable {
    private List<BossDrop> drops;
    private final String entityName;

    public BossDrops(List<BossDrop> drops, String entityName) {
        this.drops = drops;
        this.entityName = entityName;
    }

    public void drop(Player player){
        drop(player, player.getLocation());
    }

    public void drop(Player player, Location location){
        for (BossDrop drop : drops){
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

    public static BossDrops deserialize(Map<String, Object> map) {
        return new BossDrops((List<BossDrop>) map.get("drops"), (String) map.get("entityName"));
    }
}
