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

    private boolean isCraftingAllowed() {
        return plugin.getConfig().getBoolean("vanilla.crafting", false);
    }

    private boolean isAnvilAllowed() {
        return plugin.getConfig().getBoolean("vanilla.anvil", false);
    }

    private boolean isEnchantingAllowed() {
        return plugin.getConfig().getBoolean("vanilla.enchanting", false);
    }

    private boolean isDroppingAllowed() {
        return plugin.getConfig().getBoolean("vanilla.drop", false);
    }

    private boolean isSwapHandsAllowed() {
        return plugin.getConfig().getBoolean("vanilla.swap_hands", false);
    }


    @EventHandler(ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (isCraftingAllowed()) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Material clickedMat = event.getClickedBlock().getType();
            if (!isEnchantingAllowed() && clickedMat == Material.ENCHANTING_TABLE) {
                event.setCancelled(true);
            } else if (!isAnvilAllowed() && clickedMat.toString().contains("ANVIL")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isDroppingAllowed()) return;
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        event.setCancelled(true);
        player.sendMessage(MiniMessageUtils.miniMessage("""
                <red>You can't drop items!
                Use <gold><hover:show_text:'<yellow>Click to trade</yellow>'><click:suggest_command:'/trade '>/trade</click></hover> <red>to trade items with other players
                Use <gold><hover:show_text:'<yellow>Click to sell'><click:run_command:'/sell'>/sell</click></hover> <red>to sell items
                Use <gold><hover:show_text:'<yellow>Click to trash'><click:run_command:'/trash'>/trash</click></hover> <red>to get rid of items"""));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (isSwapHandsAllowed()) return;
        event.setCancelled(true);
    }
}
