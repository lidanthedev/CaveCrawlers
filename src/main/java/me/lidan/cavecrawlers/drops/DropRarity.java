package me.lidan.cavecrawlers.drops;

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Sound;

@Getter
public enum DropRarity {
    AUTO("", Sound.BLOCK_NOTE_BLOCK_PLING),
    RARE(ChatColor.BLUE + ChatColor.BOLD.toString() + "RARE", Sound.BLOCK_NOTE_BLOCK_PLING),
    VERY_RARE(ChatColor.DARK_PURPLE + ChatColor.BOLD.toString() + "VERY RARE", Sound.BLOCK_AMETHYST_BLOCK_BREAK),
    CRAZY_RARE(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "CRAZY RARE", Sound.ENTITY_WITHER_DEATH),
    INSANE(ChatColor.RED + ChatColor.BOLD.toString() + "INSANE", Sound.ENTITY_ENDER_DRAGON_DEATH),
    ;

    private final String message;
    private final Sound sound;

    DropRarity(String message, Sound sound) {
        this.message = message;
        this.sound = sound;
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
