package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonObject;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.potion.PotionEffectType;

public class PotionAbility extends ClickAbility{
    protected int duration; // in ticks
    protected int amplifier;
    protected PotionEffectType potionEffectType;
    protected double range; // 0 for self, otherwise range
    protected String target; // self, players, mobs, all

    public PotionAbility(String name, String description, double cost, long cooldown, int duration, int amplifier, PotionEffectType potionEffectType, double range, String target) {
        this(name, description, cost, cooldown, duration, amplifier, potionEffectType, range, target, Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK);
    }

    public PotionAbility(String name, String description, double cost, long cooldown, int duration, int amplifier, PotionEffectType potionEffectType, double range, String target, Action... allowedActions) {
        super(name, description, cost, cooldown, allowedActions);
        this.duration = duration;
        this.amplifier = amplifier;
        this.potionEffectType = potionEffectType;
        this.range = range;
        this.target = target;
    }

    @Override
    protected boolean useAbility(PlayerEvent playerEvent) {
        Player player = playerEvent.getPlayer();
        if (range == 0) {
            player.addPotionEffect(potionEffectType.createEffect(duration, amplifier));
            return true;
        }
        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity livingEntity)){
                continue;
            }
            if (target.equals("all") || (target.equals("players") && entity instanceof Player) || (target.equals("mobs") && entity instanceof Mob)) {
                livingEntity.addPotionEffect(potionEffectType.createEffect(duration, amplifier));
            }
        }
        return true;
    }

    @Override
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        PotionAbility ability = (PotionAbility) super.buildAbilityWithSettings(map);
        if (map.has("duration")) {
            ability.duration = map.get("duration").getAsInt();
        }
        if (map.has("amplifier")) {
            ability.amplifier = map.get("amplifier").getAsInt();
        }
        if (map.has("potionEffectType")) {
            ability.potionEffectType = PotionEffectType.getByName(map.get("potionEffectType").getAsString());
        }
        if (map.has("range")) {
            ability.range = map.get("range").getAsDouble();
        }
        if (map.has("target")) {
            ability.target = map.get("target").getAsString();
        }
        return ability;
    }
}
