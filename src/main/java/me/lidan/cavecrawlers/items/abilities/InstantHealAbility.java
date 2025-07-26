package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonObject;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.StatsManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
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
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        InstantHealAbility ability = (InstantHealAbility) super.buildAbilityWithSettings(map);
        if (map.has("healAmount")) {
            ability.healAmount = map.get("healAmount").getAsDouble();
        }
        if (map.has("healPercent")) {
            ability.healPercent = map.get("healPercent").getAsDouble();
        }
        ability.setDescription(getDescription(ability.healAmount, ability.healPercent));
        return ability;
    }

    @Override
    protected boolean useAbility(PlayerEvent playerEvent) {
        Player player = playerEvent.getPlayer();
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        if (player.getHealth() >= maxHealth){
            return false;
        }
        StatsManager.healPlayer(player, healAmount);
        StatsManager.healPlayerPercent(player, healPercent);
        player.getWorld().spawnParticle(Particle.HEART, player.getEyeLocation(), 1);
        return true;
    }
}
