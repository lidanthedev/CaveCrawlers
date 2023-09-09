package me.lidan.cavecrawlers.events;

import me.lidan.cavecrawlers.items.abilities.ItemAbility;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public class Potions implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!hand.hasItemMeta()) return;
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK){
            if (hand.getItemMeta().getDisplayName().contains("Potion")) {
                player.launchProjectile(ThrownPotion.class);
            }
        }
    }

    public static void givePot(Integer Level, Integer duration, String type, Player p) {
        p.sendMessage("You were given " + type + " Level: " + Level + " Duration: " + duration + " ticks");
    }
}
