package me.lidan.cavecrawlers.events;

import dev.triumphteam.gui.components.util.ItemNbt;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.damage.DamageManager;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.utils.Cooldown;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.UUID;

public class PotionsListener implements Listener {

    private final long cooldown;
    private final Cooldown<UUID> abilityCooldown;
    private final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private final BukkitScheduler scheduler = plugin.getServer().getScheduler();

    public PotionsListener(long cooldown) {
        this.cooldown = cooldown;
        this.abilityCooldown = new Cooldown<>();
        reloadWeapon();
    }

    public void reloadWeapon() {
        scheduler.runTaskTimer(plugin, bukkitTask -> {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                for (ItemStack content : onlinePlayer.getInventory().getContents()) {
                    updateWeapon(content);
                }
            }
        }, 0,60);
    }

    private static void updateWeapon(ItemStack content) {
        String ID = ItemsManager.getInstance().getIDofItemStack(content);
        if (ID != null && ID.contains("DARK_WIZARD")) {
            String chargesStr = ItemNbt.getString(content, "Charges");
            if (chargesStr == null) { // Not really needed anymore
                 ItemNbt.setString(content, "Charges", "5");
                 return;
            }
            int charges = Integer.parseInt(chargesStr);
            if(charges > 4) {
                return;
            }
            charges++;
            setDisplayCharges(content, charges);
        }
    }

    private static void setDisplayCharges(ItemStack content, int charges) {
        ItemNbt.setString(content, "Charges", Integer.toString(charges));
        ItemInfo itemInfo = ItemsManager.getInstance().getItemByID("DARK_WIZARD'S_POTION_BAG_(EMPTY)");
        String newName = itemInfo.getName();
        newName = newName.replaceAll("5/", charges + "/");
        ItemMeta meta = content.getItemMeta();
        meta.setDisplayName(newName);
        content.setItemMeta(meta);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!hand.hasItemMeta()) return;
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK){
            if (ItemsManager.getInstance().getIDofItemStackSafe(hand).contains("DARK_WIZARD")) {
                if (abilityCooldown.getCurrentCooldown(player.getUniqueId()) < cooldown){
                    return;
                }
                abilityCooldown.startCooldown(player.getUniqueId());
                String chargesStr = ItemNbt.getString(hand,"Charges");
                if (chargesStr  == null) {  // Not really needed anymore
                    chargesStr = "5";
                    ItemNbt.setString(hand,"Charges",chargesStr);
                }
                int charges = Integer.parseInt(chargesStr);
                if (charges <= 0) {
                    player.sendMessage("No Charges");
                    return;
                }
                charges--;
                setDisplayCharges(hand, charges);
                ThrownPotion potion = player.launchProjectile(ThrownPotion.class);
                potion.setItem(new ItemStack(Material.RED_CONCRETE));
                potion.addScoreboardTag("DARK_WIZARD_DAMAGE");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof ThrownPotion potion) {
            if (potion.getShooter() instanceof Player p) {
                if (potion.getScoreboardTags().contains("DARK_WIZARD_DAMAGE")) {
                    DamageManager damageManager = DamageManager.getInstance();
                    for (Entity nearbyEntity : potion.getNearbyEntities(3,1,3)) {
                        if (nearbyEntity instanceof Mob mob) {
                            damageManager.resetAttackCooldownForMob(p, mob);
                            mob.damage(1,p);
                        }
                    }
                }
            }
        }
    }

    public static void givePot(Integer Level, Integer duration, String type, Player p) {
        p.sendMessage("You were given " + type + " Level: " + Level + " Duration: " + duration + " ticks");
    }

}
