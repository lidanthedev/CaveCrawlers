package me.lidan.cavecrawlers.commands;

import me.lidan.cavecrawlers.griffin.GriffinManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Cooldown;
import revxrsal.commands.annotation.Subcommand;

import java.util.concurrent.TimeUnit;

@Command("griffin")
public class GriffinCommand {
    private final GriffinManager griffinManager = GriffinManager.getInstance();

    @Subcommand("reset")
    @Cooldown(unit = TimeUnit.MINUTES, value = 1)
    public void reset(Player sender) {
        try{
            Block block = griffinManager.generateGriffinLocation(sender);
            griffinManager.setGriffinBlock(sender, block);
            sender.sendMessage("Griffin location reset!");
        }
        catch (IllegalArgumentException e){
            sender.sendMessage("You are not in the correct world!");
        }
    }
}
