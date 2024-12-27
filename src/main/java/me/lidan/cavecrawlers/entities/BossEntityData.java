package me.lidan.cavecrawlers.entities;

import lombok.Getter;
import me.lidan.cavecrawlers.bosses.BossDrops;
import me.lidan.cavecrawlers.bosses.BossManager;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import me.lidan.cavecrawlers.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Getter
public class BossEntityData extends EntityData {
    private static final Logger log = LoggerFactory.getLogger(BossEntityData.class);
    protected final Map<UUID, Integer> points = new HashMap<>();
    protected long startTime = System.currentTimeMillis();
    private final List<Runnable> onDeathRunnable = new ArrayList<>();

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

    public void addOnDeathRunnable(Runnable runnable) {
        onDeathRunnable.add(runnable);
    }

    @Override
    public void onDeath(EntityDeathEvent event) {
        for (Runnable runnable : onDeathRunnable) {
            runnable.run();
        }
        String name = entity.getName();
        BossDrops drops = BossManager.getInstance().getEntityDrops(name);
        if (drops == null) return;
        List<Integer> bonusPoints = drops.getBonusPoints();
        List<Map.Entry<UUID, Double>> sortedDamage = new ArrayList<>(damageMap.entrySet().stream()
                .sorted((o1, o2) -> (int) (o2.getValue() - o1.getValue()))
                .toList());
        for (int i = 0; i < Math.min(bonusPoints.size(), sortedDamage.size()); i++) {
            addPoints(sortedDamage.get(i).getKey(), bonusPoints.get(i));
        }
        Map<String, String> placeholders = new HashMap<>();

        for (int i = 0; i < bonusPoints.size(); i++) {
            int placement = i + 1;
            placeholders.put("leaderboard_" + placement + "_name", "N/A");
            placeholders.put("leaderboard_" + placement + "_points", "N/A");
            placeholders.put("leaderboard_" + placement + "_damage", "N/A");
        }

        for (int i = 0; i < sortedDamage.size(); i++) {
            Player player = Bukkit.getPlayer(sortedDamage.get(i).getKey());
            if (player == null) continue;
            int playerPoints = getPoints(player.getUniqueId());
            Double damage = sortedDamage.get(i).getValue();
            int placement = i + 1;
            log.info("#{} {} Dealt {} damage (points {})", placement, player.getName(), damage, playerPoints);
            drops.drop(player, playerPoints);
            placeholders.put("leaderboard_" + placement + "_name", player.getDisplayName());
            placeholders.put("leaderboard_" + placement + "_points", String.valueOf(playerPoints));
            placeholders.put("leaderboard_" + placement + "_damage", StringUtils.getNumberFormat(damage));
        }
        placeholders.put("boss_name", name);
        placeholders.put("boss_time", String.valueOf((System.currentTimeMillis() - startTime) / 1000));
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            placeholders.put("attacker", killer.getDisplayName());
        }
        else{
            placeholders.put("attacker", "N/A");
        }
        ConfigMessage announce = drops.getAnnounce();
        if (announce == null) return;
        for (Player player : entity.getWorld().getPlayers()) {
            placeholders.put("player_damage", StringUtils.getNumberFormat(damageMap.getOrDefault(player.getUniqueId(), 0.0)));
            announce.sendMessage(player, placeholders);
        }
    }
}
