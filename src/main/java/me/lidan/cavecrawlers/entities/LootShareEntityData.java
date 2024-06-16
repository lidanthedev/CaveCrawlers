package me.lidan.cavecrawlers.entities;

import lombok.ToString;
import me.lidan.cavecrawlers.drops.DropsManager;
import me.lidan.cavecrawlers.drops.EntityDrops;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ToString(callSuper = true)
public class LootShareEntityData extends EntityData{
    public final static ConfigMessage LOOT_SHARE_MESSAGE = ConfigMessage.getMessageOrDefault("loot-share-message", "&9&lLoot Share for helping %summoner%!");
    private static final Logger log = LoggerFactory.getLogger(LootShareEntityData.class);
    private final int damageThresholdPercent;
    private final UUID summoner;

    public LootShareEntityData(LivingEntity entity, int damageThresholdPercent, UUID summoner) {
        super(entity);
        this.damageThresholdPercent = damageThresholdPercent;
        this.summoner = summoner;
    }

    @Override
    public void onDeath(EntityDeathEvent event) {
        String name = entity.getName();
        EntityDrops drops = DropsManager.getInstance().getEntityDrops(name);
        if (drops == null) return;
        Map<String, String> placeholders = new HashMap<>();
        double damageThreshold = entity.getMaxHealth() / 100 * damageThresholdPercent;
        if (summoner != null) {
            placeholders.put("summoner", Bukkit.getOfflinePlayer(summoner).getName());
            Double summonerDamage = damageMap.getOrDefault(summoner, 0.0);
            if (summonerDamage < damageThreshold){
                // make sure the summoner gets the loot
                damageMap.put(summoner, damageThreshold);
            }
        }
        for (Map.Entry<UUID, Double> entry : damageMap.entrySet()) {
            if (entry.getValue() >= damageThreshold) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null) continue;
                if (!entry.getKey().equals(summoner)) {
                    LOOT_SHARE_MESSAGE.sendMessage(player, placeholders);
                }
                drops.roll(player);
            }
        }
    }
}
