package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.damage.DamageCalculation;
import me.lidan.cavecrawlers.damage.DamageManager;
import me.lidan.cavecrawlers.damage.FinalDamageCalculation;
import me.lidan.cavecrawlers.damage.PlayerDamageCalculation;
import me.lidan.cavecrawlers.stats.*;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;

public class ShortBowAbility extends ClickAbility {
    public ShortBowAbility() {
        super("Short Bow", "Instantly Shoots", 0, 50, Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK, Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK);
    }

    @Override
    public void activateAbility(PlayerEvent playerEvent){
        Player player = playerEvent.getPlayer();
        Stats stats = StatsManager.getInstance().getStats(player);
        Stat attackSpeedStat = stats.get(StatType.ATTACK_SPEED);

        long attackCooldown = DamageManager.calculateAttackSpeed((long) attackSpeedStat.getValue());

        if (getAbilityCooldown().getCurrentCooldown(player.getUniqueId()) < attackCooldown){
            abilityFailedCooldown(player);
            return;
        }
        Stat manaStat = stats.get(StatType.MANA);
        if (manaStat.getValue() < getCost()){
            abilityFailedNoMana(player);
            return;
        }

        getAbilityCooldown().startCooldown(player.getUniqueId());
        manaStat.setValue(manaStat.getValue() - getCost());
        String msg = ChatColor.GOLD + getName() + "!" + ChatColor.AQUA + " (%s Mana)".formatted((int)getCost());
        ActionBarManager.getInstance().actionBar(player, msg);
        useAbility(playerEvent);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (hasAbility(event.getBow())){
            event.setCancelled(true);
        }
    }

    @Override
    protected void useAbility(PlayerEvent playerEvent) {
        Player player = playerEvent.getPlayer();
        DamageCalculation calculation = new PlayerDamageCalculation(player);
        DamageManager.getInstance().launchProjectile(player, Arrow.class, calculation);
    }

    @Override
    public void abilityFailedCooldown(Player player) {

    }

    @Override
    public List<String> toList(){
        List<String> list = new ArrayList<>();
        list.add(ChatColor.GOLD + "Item Ability: " + getName());
        list.addAll(StringUtils.loreBuilder(getDescription()));
        return list;
    }
}
