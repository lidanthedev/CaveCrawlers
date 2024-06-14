package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.perks.Perk;
import me.lidan.cavecrawlers.perks.PerksManager;
import me.lidan.cavecrawlers.stats.StatsCalculateEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;

public class PerksListener implements Listener {
    PerksManager perksManager = PerksManager.getInstance();

    @EventHandler(ignoreCancelled = true)
    public void onStatsCalculate(StatsCalculateEvent event) {
        Player player = event.getPlayer();
        Map<String, Perk> perks = perksManager.getPerks(player);
        for (Perk perk : perks.values()) {
            event.getStats().add(perk.getStats());
        }
    }
}
