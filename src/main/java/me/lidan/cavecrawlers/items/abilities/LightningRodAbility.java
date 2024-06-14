package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonObject;
import me.lidan.cavecrawlers.damage.AbilityDamage;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.utils.BukkitUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;

public class LightningRodAbility extends ScalingClickAbility {
    private int radius;

    public LightningRodAbility() {
        super("Lightning", "Strike Lightning on click", 10, 5000, StatType.INTELLIGENCE, 100, 10);
        radius = 5;
    }

    @Override
    protected boolean useAbility(PlayerEvent playerEvent) {
        Player player = playerEvent.getPlayer();
        AbilityDamage calculation = getDamageCalculation(player);

        Location target = player.getTargetBlock(null, 20).getLocation();
        player.getWorld().strikeLightningEffect(target);
        BukkitUtils.getNearbyMobs(target, radius).forEach(entity -> {
            calculation.damage(player, entity);
        });
        return true;
    }

    @Override
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        LightningRodAbility ability = (LightningRodAbility) super.buildAbilityWithSettings(map);
        if (map.has("radius")) {
            ability.radius = map.get("radius").getAsInt();
        }
        return ability;
    }
}
