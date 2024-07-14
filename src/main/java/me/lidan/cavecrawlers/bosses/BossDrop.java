package me.lidan.cavecrawlers.bosses;

import lombok.Getter;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Getter
public class BossDrop extends Drop implements ConfigurationSerializable {
    private static final Logger log = LoggerFactory.getLogger(BossDrop.class);
    private int requiredPoints;
    private String track;

    public BossDrop(String type, double chance, String value, int requiredPoints, String track, ConfigMessage announce) {
        super(type, chance, value, announce);
        this.requiredPoints = requiredPoints;
        this.track = track;
    }

    public BossDrop(String type, double chance, String value, int requiredPoints) {
        this(type, chance, value, requiredPoints, null, null);
    }

    public BossDrop(String type, double chance, String value) {
        this(type, chance, value, 0, null, null);
    }

    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("requiredPoints", requiredPoints);
        if (track != null) {
            map.put("track", track);
        }
        return map;
    }

    @Override
    protected void giveItem(Player player) {
        super.giveItem(player);
        announceToPlayersInSameWorld(player, placeholders);
        log.info("Player {} got a drop: {}", player.getName(), value);
    }

    @Override
    protected Entity giveMob(Player player, Location location) {
        Entity entity = super.giveMob(player, location);
        announceToPlayersInSameWorld(player, placeholders);
        return entity;
    }

    @Override
    protected void giveCoins(Player player) {
        super.giveCoins(player);
        announceToPlayersInSameWorld(player, placeholders);
    }

    public void announceToPlayersInSameWorld(Player player, Map<String, String> placeholders) {
        if (announce == null) {
            log.warn("No announce message for drop: {}", value);
            return;
        }
        Bukkit.getScheduler().runTaskLater(CaveCrawlers.getInstance(), () -> {
            placeholders.put("player", player.getDisplayName());
            log.info("Announcing to players in the same world: {} with placeholders: {}", player.getWorld().getName(), placeholders);
            for (Player p : player.getWorld().getPlayers()) {
                announce.sendMessage(p, placeholders);
            }
        }, 20L);
    }

    @Override
    protected void sendAnnounceMessage(Player player) {

    }

    public static BossDrop deserialize(Map<String, Object> map) {
        String type = (String) map.get("type");
        double chance = (double) map.get("chance");
        String value = (String) map.get("value");
        int requiredPoints = (int) map.get("requiredPoints");
        String track = (String) map.get("track");
        ConfigMessage announce = ConfigMessage.getMessage((String) map.get("announce"));
        return new BossDrop(type, chance, value, requiredPoints, track, announce);
    }
}
