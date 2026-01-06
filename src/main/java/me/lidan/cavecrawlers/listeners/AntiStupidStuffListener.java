package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class AntiStupidStuffListener implements Listener {

    public static final CaveCrawlers plugin = CaveCrawlers.getInstance();
    public static final boolean ALLOW_CRAFTING = plugin.getConfig().getBoolean("vanilla.crafting", false);
    public static final boolean ALLOW_ANVIL = plugin.getConfig().getBoolean("vanilla.anvil", false);
    public static final boolean ALLOW_ENCHANTING = plugin.getConfig().getBoolean("vanilla.enchanting", false);
    public static final boolean ALLOW_DROP = plugin.getConfig().getBoolean("vanilla.drop", false);
    public static final boolean ALLOW_SWAP_HANDS = plugin.getConfig().getBoolean("vanilla.swap_hands", false);


    @EventHandler(ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (ALLOW_CRAFTING) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Material clickedMat = event.getClickedBlock().getType();
            if (!ALLOW_ENCHANTING && clickedMat == Material.ENCHANTING_TABLE) {
                event.setCancelled(true);
            } else if (!ALLOW_ANVIL && clickedMat.toString().contains("ANVIL")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (ALLOW_DROP) return;
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        event.setCancelled(true);
        player.sendMessage(MiniMessageUtils.miniMessage("""
                <red>You can't drop items!
                Use <gold><hover:show_text:'<yellow>Click to trade</yellow>'><click:suggest_command:'/trade '>/trade</click></hover> <red>to trade items with other players
                Use <gold><hover:show_text:'<yellow>Click to sell'><click:run_command:'/sell'>/sell</click></hover> <red>to sell items
                Use <gold><hover:show_text:'<yellow>Click to trash'><click:run_command:'/trash'>/trash</click></hover> <red>to get rid off items"""));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (ALLOW_SWAP_HANDS) return;
        event.setCancelled(true);
    }
}
