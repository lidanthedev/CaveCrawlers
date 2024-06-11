package me.lidan.cavecrawlers.commands;

import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QolCommand {
    public static Map<UUID, Boolean> infoclickMap = new HashMap<>();

    /**
     * this command will change you form op to deop and opposite
     * must have permission vanilla op first
     * @param sender the players that run the command
     */
    @Command({"opme","deopme"})
    @CommandPermission("minecraft.command.op")
    public void opme(Player sender){
        if(sender.isOp()){
            sender.setOp(false);
            sender.sendMessage("§4§lYou are now NOT OP");
        }
        else{
            sender.setOp(true);
            sender.sendMessage("§4§lYou are now OP");
        }
    }


    /**
     * this command will show you useful info about click in inventories
     * such as: click's type, click's action (left, right, etc.), slot number, raw slot number,
     * click inv name, item name (if it has), item material, inv holder
     * the command continue in /listeners/InfoclickListener
     * @param sender the players that run the command
     */
    @Command({"infoclick","clickinfo"})
    @CommandPermission("CaveCrawles.admin.infoclick")
    public void infoclick(Player sender){
        if(infoclickMap.get(sender.getUniqueId()) != null){
            infoclickMap.remove(sender.getUniqueId());
            sender.sendMessage("§4Info click disable");
            return;
        }
        infoclickMap.put(sender.getUniqueId(), true);
        sender.sendMessage("§4Info click enable");
    }


}
