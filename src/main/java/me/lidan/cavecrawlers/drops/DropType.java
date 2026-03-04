package me.lidan.cavecrawlers.drops;

import lombok.Getter;
import org.bukkit.Material;

public enum DropType {
    ITEM(Material.DIAMOND),
    MOB(Material.ZOMBIE_HEAD),
    COINS(Material.GOLD_INGOT),
    COMMAND(Material.COMMAND_BLOCK);

    @Getter
    private final Material material;

    DropType(Material material) {
        this.material = material;
    }
}
