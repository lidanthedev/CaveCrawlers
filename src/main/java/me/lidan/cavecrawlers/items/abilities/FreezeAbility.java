package me.lidan.cavecrawlers.items.abilities;

import org.bukkit.potion.PotionEffectType;

public class FreezeAbility extends PotionAbility{
    public FreezeAbility() {
        super("Freeze", "Freeze mobs near you", 100, 10000, 40, 5, PotionEffectType.SLOW, 5, "mobs");
    }
}
