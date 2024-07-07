package me.lidan.cavecrawlers.entities;

import lombok.Getter;
import me.lidan.cavecrawlers.bosses.BossDrops;
import me.lidan.cavecrawlers.bosses.BossManager;
import me.lidan.cavecrawlers.drops.DropsManager;
import me.lidan.cavecrawlers.drops.EntityDrops;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class BossEntityData extends EntityData {
    private static final Logger log = LoggerFactory.getLogger(BossEntityData.class);
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
        BossDrops drops = BossManager.getInstance().getEntityDrops(name);
        if (drops == null) return;
        List<Map.Entry<UUID, Double>> sortedDamage = new ArrayList<>(damageMap.entrySet().stream()
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .toList());
        Collections.reverse(sortedDamage);
        for (int i = 0; i < Math.min(bonusPoints.length, sortedDamage.size()); i++) {
            addPoints(sortedDamage.get(i).getKey(), bonusPoints[i]);
        }
        log.info("BOSS DEAD: {}", name);
        for (int i = 0; i < sortedDamage.size(); i++) {
            Player player = Bukkit.getPlayer(sortedDamage.get(i).getKey());
            if (player == null) continue;
            int playerPoints = getPoints(player.getUniqueId());
            log.info("#{} {} Dealt {} damage (points {})", i+1, player.getName(), sortedDamage.get(i).getValue(), playerPoints);
            drops.drop(player, playerPoints);
        }
    }
}
