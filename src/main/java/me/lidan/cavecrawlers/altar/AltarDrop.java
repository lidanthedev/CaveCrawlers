package me.lidan.cavecrawlers.altar;

import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.DropType;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import me.lidan.cavecrawlers.stats.StatType;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.Nullable;

public class AltarDrop extends Drop implements ConfigurationSerializable {

    public AltarDrop(DropType type, double chance, String value, @Nullable ConfigMessage announce, @Nullable StatType chanceModifier, @Nullable StatType amountModifier) {
        super(type, chance, value, announce, chanceModifier, amountModifier);
    }


}
