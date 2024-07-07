package me.lidan.cavecrawlers.entities;

import lombok.Getter;
import me.lidan.cavecrawlers.drops.DropsManager;
import me.lidan.cavecrawlers.drops.EntityDrops;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class BossEntityData extends EntityData {
    protected final Map<UUID, Integer> points = new HashMap<>();
    protected int[] bonusPoints = {300, 250, 200, 150, 100};

    public BossEntityData(LivingEntity entity) {
        super(entity);
    }

    public void addPoints(UUID player, int points) {
        this.points.put(player, this.points.getOrDefault(player, 0) + points);
    }

    public int getPoints(UUID player) {
        return points.getOrDefault(player, 0);
    }

    public void setPoints(UUID player, int points) {
        this.points.put(player, points);
    }

    @Override
    public void onDeath(EntityDeathEvent event) {
        String name = entity.getName();
        EntityDrops drops = DropsManager.getInstance().getEntityDrops(name);
        if (drops == null) return;
        List<Map.Entry<UUID, Double>> sortedDamage = damageMap.entrySet().stream()
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .toList();
        for (int i = 0; i < Math.min(bonusPoints.length, sortedDamage.size()); i++) {
            addPoints(sortedDamage.get(i).getKey(), bonusPoints[i]);
        }
        for (Map.Entry<UUID, Integer> entry : points.entrySet()) {
            if (entry.getValue() > 0) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null) continue;
                drops.roll(player);
            }
        }
    }
}
