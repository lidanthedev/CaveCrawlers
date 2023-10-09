package me.lidan.cavecrawlers.mining;

import lombok.Getter;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Getter
public class BlockInfo implements ConfigurationSerializable {
    private final int blockStrength;
    private final int blockPower;

    public BlockInfo(int blockStrength, int blockPower) {
        this.blockStrength = blockStrength;
        this.blockPower = blockPower;
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("blockStrength", blockStrength);
        map.put("blockPower", blockPower);
        return map;
    }

    public static BlockInfo deserialize(Map<String, Object> map) {
        int blockStrength = (int)map.get("blockStrength");
        int blockPower = (int)map.get("blockPower");

        return new BlockInfo(blockStrength, blockPower);
    }



}
