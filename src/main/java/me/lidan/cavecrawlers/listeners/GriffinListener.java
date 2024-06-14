package me.lidan.cavecrawlers.listeners;

import io.lumine.mythic.bukkit.events.MythicMobDespawnEvent;
import me.lidan.cavecrawlers.griffin.GriffinManager;
import me.lidan.cavecrawlers.griffin.GriffinProtection;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.Rarity;
import me.lidan.cavecrawlers.items.abilities.SpadeAbility;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import me.lidan.cavecrawlers.stats.StatsManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GriffinListener implements Listener {
    private static final Logger log = LoggerFactory.getLogger(GriffinListener.class);
    private final ConfigMessage GRIFFIN_PROTECTED = ConfigMessage.getMessage("griffin_protected", "Mob Protected for %time%");
    private final ConfigMessage GRIFFIN_UNDER_LEVELED = ConfigMessage.getMessage("griffin_under_leveled", "This mob is level %griffin_level%. You are level %player_griffin_level%. Use your spade to update your level!");
    private final GriffinManager griffinManager = GriffinManager.getInstance();

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        ItemInfo itemInfo = ItemsManager.getInstance().getItemFromItemStackSafe(player.getInventory().getItemInMainHand());
        if (itemInfo == null){
            return;
        }
        if (itemInfo.getAbility() instanceof SpadeAbility){
            griffinManager.handleGriffinClick(player, event.getBlock());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        HashMap<UUID, GriffinProtection> griffinProtectionMap = griffinManager.getGriffinProtectionMap();
        griffinProtectionMap.remove(event.getEntity().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity attacker = event.getDamager();;
        if (attacker instanceof Projectile projectile) {
            attacker = (Entity) projectile.getShooter();
        }
        Entity victim = event.getEntity();
        if (attacker instanceof Player player && victim instanceof Mob mob){
            protectGriffinMobs(event, player, mob);
        }
        else if (victim instanceof Player player && attacker instanceof Mob mob){
            protectGriffinMobs(event, player, mob);
        }
    }

    private void protectGriffinMobs(EntityDamageByEntityEvent event, Player player, Mob mob) {
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (!griffinManager.isGriffinMob(mob)) return;
        Rarity rarity = griffinManager.getRarityMap().getOrDefault(player.getUniqueId(), Rarity.COMMON);
        int level = griffinManager.getGriffinMobLevel(mob.getName());
        if (level > rarity.getLevel()){
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("griffin_level", String.valueOf(level));
            placeholders.put("player_griffin_level", String.valueOf(rarity.getLevel()));
            GRIFFIN_UNDER_LEVELED.sendMessage(player, placeholders);
            event.setDamage(0);
            event.setCancelled(true);
            mob.setTarget(null);
            StatsManager.healPlayerPercent(player, 100);
            return;
        }
        GriffinProtection griffinProtection = griffinManager.getGriffinProtectionMap().get(mob.getUniqueId());
        long currentTime = System.currentTimeMillis();
        if (griffinProtection != null && !griffinProtection.isSummoner(player.getUniqueId()) && griffinProtection.isProtected(currentTime)){
            double diff = griffinProtection.getRemainingProtectionTime(currentTime) / 1000.0;
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("time", String.valueOf(diff));
            GRIFFIN_PROTECTED.sendMessage(player, placeholders);
            event.setDamage(0);
            event.setCancelled(true);
            mob.setTarget(null);
            StatsManager.healPlayerPercent(player, 100);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMythicMobDespawn(MythicMobDespawnEvent event) {
        Entity entity = event.getEntity();
        if (griffinManager.isGriffinMob(entity)){
            griffinManager.getGriffinProtectionMap().remove(entity.getUniqueId());
        }
    }
}
