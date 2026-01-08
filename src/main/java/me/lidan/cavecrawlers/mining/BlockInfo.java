package me.lidan.cavecrawlers.mining;

import lombok.Getter;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.DropType;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemType;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.stats.StatType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class BlockInfo implements ConfigurationSerializable {
    public static final @NotNull BlockData DEFAULT_REPLACEMENT_BLOCK = Material.BLACK_WOOL.createBlockData();
    private final int blockStrength;
    private final int blockPower;
    private final ItemType brokenBy;
    private final List<Drop> drops;
    private final BlockData replacementBlockData;

    public BlockInfo(int blockStrength, int blockPower, List<Drop> drops, ItemType brokenBy, BlockData replacementBlockData) {
        this.blockStrength = blockStrength;
        this.blockPower = blockPower;
        this.drops = drops;
        this.brokenBy = brokenBy;
        this.replacementBlockData = replacementBlockData;
    }

    public BlockInfo(int blockStrength, int blockPower, List<Drop> drops) {
        this(blockStrength, blockPower, drops, ItemType.PICKAXE, DEFAULT_REPLACEMENT_BLOCK);
    }

    public static BlockInfo deserialize(Map<String, Object> map) {
        int blockStrength = (int) map.getOrDefault("blockStrength", 0);
        int blockPower = (int) map.getOrDefault("blockPower", 0);
        List<Drop> drops = new ArrayList<>();
        try {
            // Legacy support for serialized drops as lists
            Map<String, Integer> itemIdMap = (Map<String, Integer>) map.getOrDefault("drops", new HashMap<String, Integer>());
            Map<ItemInfo, Integer> dropsMap = ItemsManager.getInstance().stringMapToItemMap(itemIdMap);
            for (Map.Entry<ItemInfo, Integer> entry : dropsMap.entrySet()) {
                drops.add(new Drop(DropType.ITEM, 100, entry.getKey().getID() + " " + entry.getValue(), null, null, StatType.MINING_FORTUNE));
            }
        } catch (ClassCastException e) {
            drops = (List<Drop>) map.getOrDefault("drops", List.of());
        }

        ItemType brokenBy = ItemType.valueOf((String) map.getOrDefault("brokenBy", ItemType.PICKAXE.name()));
        BlockData replacementBlockData = map.get("replacementBlockData") != null ? Bukkit.createBlockData((String) map.get("replacementBlockData")) : DEFAULT_REPLACEMENT_BLOCK;
        return new BlockInfo(blockStrength, blockPower, drops, brokenBy, replacementBlockData);
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("blockStrength", blockStrength);
        map.put("blockPower", blockPower);
        map.put("drops", drops);
        map.put("brokenBy", brokenBy.name());
        map.put("replacementBlockData", replacementBlockData.getAsString());
        return map;
    }
}
