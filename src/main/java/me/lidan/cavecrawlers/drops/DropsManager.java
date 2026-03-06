package me.lidan.cavecrawlers.drops;

import lombok.Getter;
import me.lidan.cavecrawlers.api.DropsAPI;
import me.lidan.cavecrawlers.utils.BoostedCustomConfig;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DropsManager implements DropsAPI {
    private static DropsManager instance;

    @Getter
    private final Map<String, EntityDrops> entityDropsMap = new HashMap<>();

    @Override
    public void register(String entityName, EntityDrops entityDrops){
        entityName = ChatColor.translateAlternateColorCodes('&', entityName);
        entityDropsMap.put(entityName, entityDrops);
    }

    @Override
    public EntityDrops getEntityDrops(String entityName){
        return entityDropsMap.get(entityName);
    }

    @Override
    public void updateEntityDrops(String key, EntityDrops updated) {
        entityDropsMap.put(key, updated);
        BoostedCustomConfig config = DropLoader.getInstance().getConfig(key);
        config.set(key, updated);
        config.save();
    }

    public void rollDropsForPlayer(Player player, List<Drop> drops) {
        for (Drop drop : drops) {
            drop.roll(player);
        }
    }

    public void clear(){
        entityDropsMap.clear();
    }

    public static DropsManager getInstance() {
        if (instance == null){
            instance = new DropsManager();
        }
        return instance;
    }
}
