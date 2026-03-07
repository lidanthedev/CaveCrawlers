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

    /**
     * Renames an entity drop table from oldKey to newKey.
     * Removes the old entry and saves under the new key.
     *
     * @param oldKey  the old entity name key
     * @param newKey  the new entity name key
     * @param updated the updated EntityDrops
     */
    public void renameEntityDrops(String oldKey, String newKey, EntityDrops updated) {
        if (!oldKey.equals(newKey) && entityDropsMap.containsKey(newKey)) {
            throw new IllegalArgumentException("Cannot rename drop table: key '%s' already exists".formatted(newKey));
        }
        if (!oldKey.equals(newKey)) {
            entityDropsMap.remove(oldKey);
            DropLoader.getInstance().remove(oldKey);
        }
        updateEntityDrops(newKey, updated);
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
