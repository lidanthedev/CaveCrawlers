package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.commands.QolCommand;
import me.lidan.cavecrawlers.utils.JsonMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * more info in /commands/QolCommand in "infoclick" function
 */
public class InfoclickListener implements Listener {
    @EventHandler()
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if(QolCommand.infoclickMap.getOrDefault(player.getUniqueId(),false)) {
            if (event.getClickedInventory() == null){return;}
            player.sendMessage("§6-===Info Click===-");
            JsonMessage message = new JsonMessage()
                    .append(String.format("§etype=%s ", event.getClick())).setClickAsSuggestCmd(String.valueOf(event.getClick())).save()
                    .append(String.format("§eaction=%s ", event.getAction())).setClickAsSuggestCmd(String.valueOf(event.getAction())).save()
                    .append(String.format("§eslot=%s ", event.getSlot())).setClickAsSuggestCmd(String.valueOf(event.getSlot())).save()
                    .append(String.format("§erawSlot=%s ", event.getRawSlot())).setClickAsSuggestCmd(String.valueOf(event.getRawSlot())).save()
                    .append(String.format("§eclickedInvName=%s ", event.getView().getTitle())).setClickAsSuggestCmd(event.getView().getTitle()).save();
                    if(event.getCurrentItem() != null) {
                        if(event.getCurrentItem().getItemMeta() != null) {
                            if(!event.getCurrentItem().getItemMeta().getDisplayName().equals("")) {
                                message.append(String.format("§eclickedItem=%s ", event.getCurrentItem().getItemMeta().getDisplayName())).setClickAsSuggestCmd(event.getCurrentItem().getItemMeta().getDisplayName().replace("§","&")).save();
                            }
                        }
                        message.append(String.format("§eclickedItemMaterial=%s ", event.getCurrentItem().getType())).setClickAsSuggestCmd(String.valueOf(event.getCurrentItem().getType())).save();
                    }
                    message.append(String.format("§einventoryHolder=%s ", event.getClickedInventory().getHolder())).setClickAsSuggestCmd(String.valueOf(event.getClickedInventory().getHolder())).save()
                    .send(player);
            player.sendMessage("§6-==============-");
        }
    }
}
