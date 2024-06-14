package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonObject;
import me.lidan.cavecrawlers.damage.DamageCalculation;
import me.lidan.cavecrawlers.damage.DamageManager;
import me.lidan.cavecrawlers.damage.PlayerDamageCalculation;
import me.lidan.cavecrawlers.utils.BukkitUtils;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.Vector;


public class ShortMultiShotAbility extends ShortBowAbility {
    public static final String BOW_TAG = "MULTI_SHOT";
    private MultiShotAbility multiShotAbility;

    public ShortMultiShotAbility(int amount) {
        this(amount, 1000L);
    }

    public ShortMultiShotAbility(int amount, long maxPowerTime) {
        this(amount, maxPowerTime, 3, 5);
    }

    public ShortMultiShotAbility(int amount, long maxPowerTime, double maxPower, int yawDiff) {
        this.multiShotAbility = new MultiShotAbility(amount, maxPowerTime, maxPower, yawDiff);
    }

    @Override
    protected boolean useAbility(PlayerEvent playerEvent) {
        shoot(playerEvent.getPlayer(), 1);
        return true;
    }

    public void shoot(Player player, double force) {
        multiShotAbility.shoot(player, force);
    }

    @Override
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        ShortMultiShotAbility ability = (ShortMultiShotAbility) super.buildAbilityWithSettings(map);
        ability.multiShotAbility = (MultiShotAbility) multiShotAbility.buildAbilityWithSettings(map);
        return ability;
    }
}
