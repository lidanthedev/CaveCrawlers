package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.griffin.GriffinManager;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.Rarity;
import me.lidan.cavecrawlers.items.abilities.SpadeAbility;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class GriffinListener implements Listener {
    GriffinManager griffinManager = GriffinManager.getInstance();

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        ItemInfo itemInfo = ItemsManager.getInstance().getItemFromItemStackSafe(player.getInventory().getItemInMainHand());
        if (itemInfo == null){
            return;
        }
        if (itemInfo.getAbility() instanceof SpadeAbility){
            griffinManager.handleGriffinClick(player, event.getBlock());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity attacker = event.getDamager();
        Entity victim = event.getEntity();
        if (attacker instanceof Player player){
            protectGriffinMobs(event, player, victim);
        }
        else if (victim instanceof Player player){
            protectGriffinMobs(event, player, attacker);
        }
    }

    private void protectGriffinMobs(EntityDamageByEntityEvent event, Player player, Entity mob) {
        if (player.getGameMode() == GameMode.CREATIVE) return;
        Rarity rarity = griffinManager.getRarityMap().get(player.getUniqueId());
        if (rarity == null){
            rarity = Rarity.COMMON;
        }
        if (griffinManager.isGriffinMob(mob)){
            int level = griffinManager.getGriffinMobLevel(mob.getName());
            if (level > rarity.getLevel()){
                player.sendMessage(ChatColor.RED + "You are under-leveled to fight this mob!");
                player.sendMessage(ChatColor.RED + "Use your spade to update your level!");
                event.setCancelled(true);
            }
        }
    }
}
