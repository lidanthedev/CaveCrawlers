package me.lidan.cavecrawlers.levels;

import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class DeathXP implements Listener {
/// Test only going to disable later
    private final PlayerLevelManager playerLevelManager;

    public DeathXP(PlayerLevelManager playerLevelManager) {
        this.playerLevelManager = playerLevelManager;
    }

    @EventHandler
    public void onEndermanDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Enderman) {
            if (event.getEntity().getKiller() != null) {
                Player player = event.getEntity().getKiller();
                // Adjust XP per Enderman as needed
                int xpPerEnderman = 5;
                playerLevelManager.giveXPForKillingEndermen(player, xpPerEnderman);
            }
        }
    }
}