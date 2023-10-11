package me.lidan.cavecrawlers.drops;

import net.md_5.bungee.api.ChatColor;

public enum DropRarity {
    AUTO(""),
    RARE(ChatColor.BLUE + ChatColor.BOLD.toString() + "RARE"),
    VERY_RARE(ChatColor.DARK_PURPLE + ChatColor.BOLD.toString() + "VERY RARE"),
    CRAZY_RARE(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "CRAZY RARE"),
    INSANE(ChatColor.RED + ChatColor.BOLD.toString() + "INSANE"),
    ;

    private final String message;

    DropRarity(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message + " DROP! ";
    }

    public static DropRarity getRarity(double chances) {
        if (chances <= 0.01)
            return DropRarity.INSANE;
        else if (chances <= 0.2)
            return DropRarity.CRAZY_RARE;
        else if (chances <= 1)
            return DropRarity.VERY_RARE;
        return DropRarity.RARE;
    }
}
