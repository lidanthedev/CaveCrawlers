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

    @Override
    protected boolean useAbility(PlayerEvent event) {
        Player player = event.getPlayer();

        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        BukkitUtils.teleportForward(player, blocks);
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
