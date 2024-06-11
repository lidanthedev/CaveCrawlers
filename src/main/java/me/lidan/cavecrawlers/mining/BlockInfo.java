package me.lidan.cavecrawlers.mining;

import lombok.Getter;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemType;
import me.lidan.cavecrawlers.items.ItemsManager;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Getter
public class BlockInfo implements ConfigurationSerializable {
    private final int blockStrength;
    private final int blockPower;
    private final ItemType brokenBy;
    private final Map<ItemInfo, Integer> drops;

    public BlockInfo(int blockStrength, int blockPower, Map<ItemInfo, Integer> drops, ItemType brokenBy) {
        this.blockStrength = blockStrength;
        this.blockPower = blockPower;
        this.drops = drops;
        this.brokenBy = brokenBy;
    }

    public BlockInfo(int blockStrength, int blockPower, Map<ItemInfo, Integer> drops) {
        this(blockStrength, blockPower, drops, ItemType.PICKAXE);
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        ItemsManager itemsManager = ItemsManager.getInstance();
        Map<String, Object> map = new HashMap<>();
        map.put("blockStrength", blockStrength);
        map.put("blockPower", blockPower);
        map.put("drops", itemsManager.itemMapToStringMap(drops));
        map.put("brokenBy", brokenBy.name());
        return map;
    }

    public static BlockInfo deserialize(Map<String, Object> map) {
        int blockStrength = (int)map.get("blockStrength");
        int blockPower = (int)map.get("blockPower");
        Map<String, Integer> itemIdMap = (Map<String, Integer>) map.getOrDefault("drops", new HashMap<String, Integer>());
        Map<ItemInfo, Integer> drops = ItemsManager.getInstance().stringMapToItemMap(itemIdMap);
        ItemType brokenBy = ItemType.valueOf((String) map.getOrDefault("brokenBy", ItemType.PICKAXE.name()));
        return new BlockInfo(blockStrength, blockPower, drops, brokenBy);
    }



}
