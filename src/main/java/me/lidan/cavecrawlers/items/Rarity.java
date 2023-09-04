package me.lidan.cavecrawlers.items;

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum Rarity {
    NONE(0, ChatColor.DARK_GRAY),
    COMMON(1, ChatColor.WHITE),
    UNCOMMON(2, ChatColor.GREEN),
    RARE(3, ChatColor.BLUE),
    EPIC(4, ChatColor.DARK_PURPLE),
    LEGENDARY(5, ChatColor.GOLD),
    MYTHIC(6, ChatColor.LIGHT_PURPLE),
    DIVINE(7, ChatColor.AQUA),
    SPECIAL(8, ChatColor.RED);

    private final int level;
    private final ChatColor color;

    Rarity(int level, ChatColor color) {
        this.level = level;
        this.color = color;
    }

    public Rarity add(int adder) {
        if (this.ordinal() + adder < 0) return Rarity.NONE;
        if (this.ordinal() + adder >= values().length) return Rarity.SPECIAL;

        return values()[this.ordinal() + adder];
    }

    @Override
    public String toString() {
        return this.getColor() + "" + ChatColor.BOLD + this.name();
    }

    public static List<Rarity> getRarities() {
        return Arrays.stream(values()).filter(r -> r != NONE).collect(Collectors.toList());
    }

    public static Rarity getRarity(int level) {
        for (Rarity rarity : values()) {
            if (rarity.getLevel() == level) {
                return rarity;
            }
        }
        return NONE;
    }

    public static Rarity getRarity(ChatColor color) {
        for (Rarity rarity : values()) {
            if (rarity.getColor().equals(color)) {
                return rarity;
            }
        }
        return NONE;
    }
}
