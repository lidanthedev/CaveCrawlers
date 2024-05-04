package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.utils.BukkitUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;

public class TransmissionAbility extends ClickAbility{
    private final double blocks;

    public TransmissionAbility(double blocks) {
        super("Transmission", "Teleport ahead of you", 50, 50);
        this.blocks = blocks;
    }

    @Override
    protected void useAbility(PlayerEvent event) {
        Player player = event.getPlayer();

        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        BukkitUtils.teleportForward(player, blocks);
    }

    @Override
    public void abilityFailedCooldown(Player player) {
        // silent cooldown
    }
}
