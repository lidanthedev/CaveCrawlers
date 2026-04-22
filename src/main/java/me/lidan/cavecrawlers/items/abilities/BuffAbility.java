package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonObject;
import me.lidan.cavecrawlers.objects.SoundOptions;
import me.lidan.cavecrawlers.stats.Stat;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsCalculateEvent;
import me.lidan.cavecrawlers.utils.Cooldown;
import me.lidan.cavecrawlers.utils.StringUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;

public class BuffAbility extends ClickAbility {
    private Cooldown<UUID> chargeCooldown = new Cooldown<>();
    private Sound sound = Sound.ENTITY_WOLF_GROWL;
    private int activeTime = 10000;
    private int amount = 10;
    private StatType statType = StatType.STRENGTH;

    public BuffAbility() {
        super("Buff", StringUtils.colored("Increase your %stat%&7 by &a%amount%&7 for &a%time% seconds"), 20, 50000);
    }

    @Override
    public String getDescription() {
        Map<String, Object> placeholders = Map.of(
                "stat", statType.getFormatName(),
                "amount", amount,
                "time", activeTime / 1000
        );
        return StringUtils.applyCustomPlaceholders(super.getDescription(), placeholders);
    }

    @Override
    protected boolean useAbility(PlayerEvent playerEvent) {
        Player player = playerEvent.getPlayer();
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 100);
        chargeCooldown.setCooldown(player.getUniqueId(), System.currentTimeMillis());
        player.playSound(player.getLocation(), sound, 1, 2);
        return true;
    }

    @Override
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        BuffAbility ability = (BuffAbility) super.buildAbilityWithSettings(map);
        if (map.has("amount")) {
            ability.amount = map.get("amount").getAsInt();
        }
        if (map.has("activeTime")) {
            ability.activeTime = map.get("activeTime").getAsInt();
        }
        if (map.has("statType")) {
            ability.statType = StatType.valueOf(map.get("statType").getAsString());
        }
        if (map.has("sound")) {
            ability.sound = SoundOptions.resolveSound(map.get("sound").getAsString());
        }
        return ability;
    }

    @EventHandler
    public void onStatsUpdate(StatsCalculateEvent event) {
        Player player = event.getPlayer();
        Stats stats = event.getStats();
        if (System.currentTimeMillis() - chargeCooldown.getCooldown(player.getUniqueId()) >= activeTime) {
            return;
        }
        Stat stat = stats.get(statType);
        stat.add(amount);
    }

    @Override
    public ItemAbility clone() {
        BuffAbility clone = (BuffAbility) super.clone();
        clone.chargeCooldown = new Cooldown<>();
        clone.statType = statType;
        return clone;
    }
}
