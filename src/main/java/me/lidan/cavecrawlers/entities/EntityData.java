package me.lidan.cavecrawlers.entities;

import lombok.ToString;
import me.lidan.cavecrawlers.drops.DropsManager;
import me.lidan.cavecrawlers.drops.EntityDrops;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ToString
public class EntityData {
    private static final Logger log = LoggerFactory.getLogger(EntityData.class);
    protected final LivingEntity entity;
    protected final Map<UUID, Double> damageMap = new HashMap<>();

    public EntityData(LivingEntity entity) {
        this.entity = entity;
    }

    public void addDamage(UUID uuid, double damage) {
        Double oldDamage = damageMap.getOrDefault(uuid, 0.0);
        damageMap.put(uuid, oldDamage + damage);
    }

    public double getDamage(UUID uuid) {
        return damageMap.getOrDefault(uuid, 0.0);
    }

    public void onDeath(EntityDeathEvent event) {
        Player player = entity.getKiller();
        String name = entity.getName();
        EntityDrops drops = DropsManager.getInstance().getEntityDrops(name);
        if (drops == null) return;
        if (player == null) return;
        drops.roll(player);
    }
}
