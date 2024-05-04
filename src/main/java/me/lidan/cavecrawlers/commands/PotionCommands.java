package me.lidan.cavecrawlers.commands;

import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import static me.lidan.cavecrawlers.listeners.PotionsListener.givePot;

@Command({"pot", "potion"})
@CommandPermission("cavecrawlers.test")
public class PotionCommands {
    @Subcommand("give regeneration")
    public void givePotCommandRegen(Player p,int a,int b) {
        givePot(a,b,"Regeneration", p);
    }
    @Subcommand("give healthboost")
    public void givePotCommandHealthBoost(Player p,int a,int b) {
        givePot(a,b,"Health Boost", p);
    }
}
