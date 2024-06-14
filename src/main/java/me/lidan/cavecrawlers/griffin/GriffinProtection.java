package me.lidan.cavecrawlers.griffin;

import lombok.Data;
import org.bukkit.entity.Player;

import java.util.UUID;

@Data
public class GriffinProtection {
    private long spawnTime;
    private long protectionTime;
    private UUID summoner;

    public GriffinProtection(long spawnTime, long protectionTime, UUID summoner) {
        this.spawnTime = spawnTime;
        this.protectionTime = protectionTime;
        this.summoner = summoner;
    }

    public long getRemainingProtectionTime(long currentTime) {
        return spawnTime + protectionTime - currentTime;
    }

    public boolean isProtected() {
        return isProtected(System.currentTimeMillis());
    }

    public boolean isProtected(long currentTime) {
        return currentTime < spawnTime + protectionTime;
    }

    public boolean isSummoner(UUID playerUUID) {
        return summoner.equals(playerUUID);
    }

    public boolean isSummoner(Player playerUUID) {
        return isSummoner(playerUUID.getUniqueId());
    }
}
