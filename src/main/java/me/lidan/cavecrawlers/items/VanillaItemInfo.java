package me.lidan.cavecrawlers.items;

import me.lidan.cavecrawlers.stats.Stats;
import org.bukkit.Material;

import java.util.List;

public class VanillaItemInfo extends ItemInfo {
    private final Material material;

    public VanillaItemInfo(String material) {
        this(Material.getMaterial(material));
    }

    public VanillaItemInfo(Material material) {
        super(material.name(), null, null, material, null);
        this.material = material;
    }

    @Override
    public List<String> toList() {
        return null;
    }
}
