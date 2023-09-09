package me.lidan.cavecrawlers.events;

import dev.triumphteam.gui.components.util.ItemNbt;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.utils.Cooldown;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
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
                player.sendMessage(ChargesRemoveNumber.toString());
                player.launchProjectile(ThrownPotion.class);
            }
        }
    }

    public static void givePot(Integer Level, Integer duration, String type, Player p) {
        p.sendMessage("You were given " + type + " Level: " + Level + " Duration: " + duration + " ticks");
    }

}
