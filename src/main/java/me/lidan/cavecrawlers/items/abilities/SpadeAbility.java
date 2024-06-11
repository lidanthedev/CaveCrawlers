package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonObject;
import me.lidan.cavecrawlers.griffin.GriffinManager;
import me.lidan.cavecrawlers.utils.BukkitUtils;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SpadeAbility extends ClickAbility{
    GriffinManager griffinManager = GriffinManager.getInstance();
    private int range = 100;

    public SpadeAbility() {
        super("Spade", "Line to gold", 20, 500);
    }

    @Override
    protected void useAbility(PlayerEvent playerEvent) {
        if (playerEvent instanceof PlayerInteractEvent event){
            Player player = event.getPlayer();
            Block block = griffinManager.getGriffinBlock(event.getPlayer());
            if (player.getLocation().distance(block.getLocation()) > range){
                block = griffinManager.generateGriffinLocation(player, range);
                griffinManager.setGriffinBlock(player, block);
            }
            BukkitUtils.getLineBetweenTwoPoints(player.getEyeLocation(), block.getLocation().add(0.5,0.5,0.5), 1, loc -> {
                player.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0);
            });
            player.sendBlockChange(block.getLocation(), Material.GOLD_BLOCK.createBlockData());
        }
    }

    @Override
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        SpadeAbility ability = (SpadeAbility) super.buildAbilityWithSettings(map);
        if (map.has("range")) {
            ability.range = map.get("range").getAsInt();
        }
        return ability;
    }
}
