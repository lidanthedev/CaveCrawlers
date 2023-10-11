package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.damage.FinalDamageCalculation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;

import java.util.List;

public class FinalBoomAbility extends ClickAbility implements Listener {

    public FinalBoomAbility() {
        super("BOOM BOOM", "Does BOOM", 0, 100);
    }

    @Override
    protected void useAbility(PlayerEvent playerEvent) {
        Player player = playerEvent.getPlayer();
        List<Entity> nearbyEntities = player.getNearbyEntities(3, 3, 3);
        double damage = 10000;
        FinalDamageCalculation calculation = new FinalDamageCalculation(damage, false);
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Mob mob){
                calculation.damage(player, mob);
            }
        }
    }

    @Override
    public void abilityFailedCooldown(Player player) {
        // silent cooldown
    }
}
