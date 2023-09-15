package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.StatsManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class InstantHealAbility extends ChargedItemAbility implements Listener {
    private double healAmount;
    private double healPercent;

    public InstantHealAbility(double cost, int maxCharges, long chargeTime, double healAmount, double healPercent) {
        super("Instant Heal", getDescription(healAmount, healPercent), cost, maxCharges, chargeTime);
        this.healAmount = healAmount;
        this.healPercent = healPercent;
    }

    @NotNull
    private static String getDescription(double healAmount, double healPercent) {

        String description = "";

        StatType health = StatType.HEALTH;
        if (healAmount > 0 && healPercent > 0){
            description += healAmount + ChatColor.GRAY.toString() + " + " + health.getColor() + healPercent + "%";
        }
        else if (healAmount > 0) {
            description += healAmount;
        } else if (healPercent > 0) {
            description += health.getColor().toString() + healPercent + "%";
        }

        description += health.getIcon();


        return "Heals you for " + health.getColor() + description;
    }

    @Override
    protected void useAbility(Player player) {
        StatsManager.healPlayer(player, healAmount);
        StatsManager.healPlayerPercent(player, healPercent);
        player.getWorld().spawnParticle(Particle.HEART, player.getEyeLocation(), 1);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hasAbility(hand)){
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK){
                activateAbility(player);
            }
        }
    }
}
