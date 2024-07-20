package me.lidan.cavecrawlers.altar;

import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.DropType;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.utils.RandomUtils;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class AltarDrop extends Drop implements ConfigurationSerializable {
    public AltarDrop(DropType type, double chance, String value, @Nullable ConfigMessage announce, @Nullable StatType chanceModifier, @Nullable StatType amountModifier) {
        super(type, chance, value, announce, chanceModifier, amountModifier);
    }

    public AltarDrop(double chance, String value) {
        super(DropType.MOB, chance, value, null, null, null);
    }

    public void roll(Location location) {
        if (rollChance()) {
            drop(location);
        }
    }

    private void drop(Location location) {
        drop(null, location);
    }

    @Override
    protected void sendAnnounceMessage(Player player) {
        if (player == null) return;
        super.sendAnnounceMessage(player);
    }

    private boolean rollChance() {
        return RandomUtils.chanceOf(chance);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return super.serialize();
    }

    public static Drop deserialize(Map<String, Object> map) {
        Drop drop = Drop.deserialize(map);
        return new AltarDrop(drop.getType(), drop.getChance(), drop.getValue(), drop.getAnnounce(), drop.getChanceModifier(), drop.getAmountModifier());
    }
}
