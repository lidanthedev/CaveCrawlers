package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.commands.QolCommand;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * more info in /commands/QolCommand in "infoclick" function
 */
public class InfoclickListener implements Listener {
    @EventHandler()
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (QolCommand.infoclickMap.getOrDefault(player.getUniqueId(), false)) {
            if (event.getClickedInventory() == null) {
                return;
            }
            player.sendMessage(Component.text("§6-===Info Click===-"));

            Map<String, Object> placeholders = new HashMap<>();
            placeholders.put("type", event.getClick());
            placeholders.put("action", event.getAction());
            placeholders.put("slot", event.getSlot());
            placeholders.put("raw-slot", event.getRawSlot());
            placeholders.put("clicked-inv-name", event.getView().getTitle());
            placeholders.put("clicked-item", "None");
            placeholders.put("clicked-item-material", "None");

            if (event.getCurrentItem() != null) {
                if (event.getCurrentItem().getItemMeta() != null && !event.getCurrentItem().getItemMeta().getDisplayName().isEmpty()) {
                    placeholders.put("clicked-item", event.getCurrentItem().getItemMeta().getDisplayName().replace("§", "&"));
                }
                placeholders.put("clicked-item-material", event.getCurrentItem().getType());
            }
            placeholders.put("inventory-holder", event.getClickedInventory().getHolder());

            Component message = MiniMessageUtils.miniMessage(
                    "<click:suggest_command:'<type>'>type=<type></click> <click:suggest_command:'<action>'>action=<action></click> <click:suggest_command:'<slot>'>slot=<slot></click> <click:suggest_command:'<raw-slot>'>rawSlot=<raw-slot></click> <click:suggest_command:'<clicked-inv-name>'>clickedInvName=<clicked-inv-name></click> <click:suggest_command:'<clicked-item>'>clickedItem=<clicked-item></click> <click:suggest_command:'<clicked-item-material>'>clickedItemMaterial=<clicked-item-material></click> <click:suggest_command:'<inventory-holder>'>inventoryHolder=<inventory-holder></click>",
                    placeholders
            );

            player.sendMessage(message);
            player.sendMessage(Component.text("§6-==============-"));
        }
    }
}
