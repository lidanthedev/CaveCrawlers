package me.lidan.cavecrawlers.events;

import dev.triumphteam.gui.components.util.ItemNbt;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.stats.ActionBarManager;
import me.lidan.cavecrawlers.stats.StatsManager;
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
import org.bukkit.metadata.MetadataValue;
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
                    String ID = ItemsManager.getInstance().getIDofItemStack(content);
                    if (ID != null && ID.contains("DARK_WIZARD")) {

                        String ChargesAddString = ItemNbt.getString(content, "Charges");
                         if (ChargesAddString == null) {
                             ItemNbt.setString(content, "Charges", "5");
                             continue;
                         }
                        Integer ChargesAddInteger = Integer.parseInt(ChargesAddString);
                        if(ChargesAddInteger > 4) {
                            continue;
                        }
                        ChargesAddInteger++;
                        ItemNbt.setString(content, "Charges", ChargesAddInteger.toString());
                        String a = ItemsManager.getInstance().getItemByID("DARK_WIZARD'S_POTION_BAG_(EMPTY)").getName();
                        a = a.replaceAll("5/", ChargesAddInteger.toString() + "/");
                        ItemMeta b = content.getItemMeta();
                        b.setDisplayName(a);
                        content.setItemMeta(b);
                    }
                }
            }
        }, 0,60);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!hand.hasItemMeta()) return;
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK){
            if (abilityCooldown.getCurrentCooldown(player.getUniqueId()) < cooldown){
                return;
            }
            String ID = ItemsManager.getInstance().getIDofItemStack(hand);
            if (ID != null && ID.contains("DARK_WIZARD")) {

                abilityCooldown.startCooldown(player.getUniqueId());
                String ChargesRemoveString = ItemNbt.getString(hand,"Charges");
                if (ChargesRemoveString  == null) {
                    ChargesRemoveString = "5";
                    ItemNbt.setString(hand,"Charges",ChargesRemoveString);
                }
                Integer ChargesRemoveNumber = Integer.parseInt(ChargesRemoveString);
                if (ChargesRemoveNumber < 1) {
                    player.sendMessage("No Charges");
                    return;
                }
                ChargesRemoveNumber--;
                ItemNbt.setString(hand,"Charges",ChargesRemoveNumber.toString());
                String a = ItemsManager.getInstance().getItemByID("DARK_WIZARD'S_POTION_BAG_(EMPTY)").getName();
                a = a.replaceAll("5/", ChargesRemoveNumber.toString() + "/");
                ItemMeta b = hand.getItemMeta();
                b.setDisplayName(a);
                hand.setItemMeta(b);
                hand.getItemMeta().setDisplayName(a);
                ThrownPotion potion = player.launchProjectile(ThrownPotion.class);
                potion.setItem(new ItemStack(Material.RED_CONCRETE));
                potion.addScoreboardTag("DARK_WIZARD'S_POTION_BAG_(EMPTY)");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof ThrownPotion potion) {
            if (potion.getShooter() instanceof Player p) {
                if (potion.getScoreboardTags().contains("DARK_WIZARD'S_POTION_BAG_(EMPTY)")) {
                    for (Entity nearbyEntity : potion.getNearbyEntities(3,1,3)) {
                        if (nearbyEntity instanceof Mob mob) {
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
