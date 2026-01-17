package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonObject;
import me.lidan.cavecrawlers.utils.BukkitUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;

public class TransmissionAbility extends ClickAbility{
    private double blocks;

    public TransmissionAbility(double blocks) {
        super("Transmission", "Teleport ahead of you", 50, 50);
        this.blocks = blocks;
    }

    public TransmissionAbility(String name, String description, double cost, long cooldown, double blocks) {
        super(name, description, cost, cooldown);
        this.blocks = blocks;
    }

    @Override
    protected boolean useAbility(PlayerEvent event) {
        Player player = event.getPlayer();
        boolean teleported = BukkitUtils.teleportForward(player, blocks);
        if (!teleported) return false;
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        return true;
    }

    @Override
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        TransmissionAbility ability = (TransmissionAbility) super.buildAbilityWithSettings(map);
        if (map.has("blocks")) {
            ability.blocks = map.get("blocks").getAsDouble();
        }
        return ability;
    }

    @Override
    public void abilityFailedCooldown(Player player) {
        // silent cooldown
    }
}
